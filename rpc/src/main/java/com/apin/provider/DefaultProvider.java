package com.apin.provider;

import com.apin.common.exception.RemotingException;
import com.apin.common.protocol.RPCProtocol;
import com.apin.common.transport.body.PublishServiceCustomBody;
import com.apin.common.utils.NamedThreadFactory;
import com.apin.provider.container.ProviderServiceContainer;
import com.apin.provider.manager.ServiceDegradeManager;
import com.apin.provider.manager.ServiceFlowManager;
import com.apin.provider.manager.ServiceMonitorManager;
import com.apin.provider.manager.ServiceRegistryManager;
import com.apin.provider.processor.ProviderInactiveProcessor;
import com.apin.provider.processor.ProviderRPCServerProcessor;
import com.apin.provider.processor.ProviderDegradeServerProcessor;
import com.apin.provider.wrapper.LocalServerWrapperManager;
import com.apin.rpc.client.NettyClientConfig;
import com.apin.rpc.client.NettyRemotingClient;
import com.apin.rpc.model.RemotingTransporter;
import com.apin.rpc.server.NettyRemotingServer;
import com.apin.rpc.server.NettyServerConfig;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

/**
 * Created by Administrator on 2017/3/17.
 */
public class DefaultProvider implements Provider {

    private static final Logger logger= LoggerFactory.getLogger(DefaultProvider.class);

    //向注册中心和监控中心连接的netty client配置
    private NettyClientConfig clientConfig;

    //等待服务消费者连接的netty server的配置
    private NettyServerConfig serverConfig;

    //连接monitor和注册中心连接的客户端
    private NettyRemotingClient nettyRemotingClient;

    //等待被Consumer连接的服务端
    private NettyRemotingServer nettyRemotingServer;

    //等待被Consumer VIP连接的服务端
    private NettyRemotingServer nettyRemotingVipServer;

    //RPC调用的核心线程执行器
    private ExecutorService remotingExecutor;

    //VIP RPC调用的核心线程执行器
    private ExecutorService remotingVipExecutor;

    //连接monitor端的channel
    private Channel monitorChannel;

    //注册中心地址
    private String registryAddress;

    //服务暴露给consumer的地址
    private int exposePort;

    //监控中心的地址
    private String monitorAddress;

    //要提供的服务
    private Object [] obj;

    //当前provider端状态是否健康，也就是说如果注册宕机(有可能是注册服务器宕机，也有可能是服务宕机)后，该provider端的实例信息在注册机上是失效的，因为默认状态下start就是发送，
    //只有当channel active的时候说明断线了，需要重新发布信息
    private boolean ProviderStateIsHealthy=true;

    //定时任务执行器
    private final ScheduledExecutorService scheduledExecutorService= Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("provider-timer"));

    public DefaultProvider(){
        this.clientConfig=new NettyClientConfig();
        this.serverConfig=new NettyServerConfig();
        initialize();
    }

    public DefaultProvider(NettyClientConfig clientConfig,NettyServerConfig serverConfig){
        this.clientConfig=clientConfig;
        this.serverConfig=serverConfig;
        initialize();
    }

    public Provider publishService(Object... obj){
        this.obj=obj;
        return this;
    }

    public Provider serviceListenPort(int exposePort) {
        this.exposePort=exposePort;
        return this;
    }

    public Provider registryAddress(String registryAddress) {
        this.registryAddress=registryAddress;
        return this;
    }

    public Provider monitorAddress(String monitorAddress) {
        this.monitorAddress=monitorAddress;
        return this;
    }

    private void initialize(){
        this.nettyRemotingServer=new NettyRemotingServer(this.serverConfig);
        this.nettyRemotingClient=new NettyRemotingClient(this.clientConfig);
        this.nettyRemotingVipServer=new NettyRemotingServer(this.serverConfig);

        this.remotingExecutor=Executors.newFixedThreadPool(serverConfig.getServerWorkerThreads(),new NamedThreadFactory("providerExecutorThread_"));
        this.remotingVipExecutor=Executors.newFixedThreadPool(serverConfig.getServerWorkerThreads()/2,new NamedThreadFactory("providerVipExecutorThread_"));
        this.registerProcessor();

        //延迟60秒，每隔60秒向注册中心发送注册服务信息
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try{
                    logger.info("schedule check publish service");
                    if(!ProviderStateIsHealthy){
                        logger.info("channel which connected to registry,has been inactive,need to republish service");
                        ServiceRegistryManager.publishServiceListToRegistry(registryAddress,nettyRemotingClient);
                    }
                }catch (Exception e){
                    logger.warn("schedule publish failed [{}]",e.getMessage());
                }
            }
        },60,60,TimeUnit.SECONDS);

        //定期检查是否有服务没有发布成功，如果没有发布成功就将其发布出去
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    logger.info("ready send message");
                    ServiceRegistryManager.checkPublishFailMessage(nettyRemotingClient);
                } catch (Exception e) {
                    logger.warn("schedule republish failed [{}]", e.getMessage());
                }
            }
        }, 1, 1, TimeUnit.MINUTES);

        /**
         * 清理所有服务的单位时间的实效过期的统计信息
         */
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                logger.info("ready prepare send Report");
                ServiceFlowManager.clearAllServiceNextMinuteCallCount();
            }
        }, 5, 45, TimeUnit.SECONDS);

        /**
         * 如果监控中心的地址不是null，则需要定时发送统计信息
         */
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                ServiceMonitorManager.sendMetricsInfo(monitorAddress, monitorChannel);
            }
        }, 5, 60, TimeUnit.SECONDS);

        /**
         * 每隔60s去校验与monitor端的channel是否健康，如果不健康，或者inactive的时候，则重新去连接
         */
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    checkMonitorChannel();
                } catch (InterruptedException e) {
                    logger.warn("schedule check monitor channel failed [{}]", e.getMessage());
                }
            }
        }, 30, 60, TimeUnit.SECONDS);

        /**
         * 检查是否有服务需要自动降级
         */
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                ServiceDegradeManager.checkAutoDegrade();
            }
        }, 30, 60, TimeUnit.SECONDS);
    }

    private void registerProcessor(){
        ProviderDegradeServerProcessor providerDegradeServerProcessor =new ProviderDegradeServerProcessor();
        //provider端作为client端去连接registry注册中心的处理器
        this.nettyRemotingClient.registerProcessor(RPCProtocol.DEGRADE_SERVICE, providerDegradeServerProcessor, null);
        this.nettyRemotingClient.registerProcessor(RPCProtocol.AUTO_DEGRADE_SERVICE, providerDegradeServerProcessor, null);
        //provider端连接registry链接inactive的时候需要进行的操作(设置注册状态为不健康，需要重新发送注册信息)
        this.nettyRemotingClient.registerChannelInactiveProcessor(new ProviderInactiveProcessor(this), null);
        //provider端作为rpc的server端去等待rpc客户端连接的处理器，此处理器只处理RPC请求
        this.nettyRemotingServer.registerDefaultProcessor(new ProviderRPCServerProcessor(this), this.remotingExecutor);
        this.nettyRemotingVipServer.registerDefaultProcessor(new ProviderRPCServerProcessor(this), this.remotingVipExecutor);
    }


    public void start() throws InterruptedException, RemotingException {
        logger.info("########## provider starting.... #######");
        ProviderServiceContainer.initPublishRemotingTransporters(LocalServerWrapperManager.wrapperRegisterInfo(this.getExposePort(), this.obj));
        logger.info("registry center address [{}] servicePort [{}] service [{}]",this.registryAddress,this.exposePort,ProviderServiceContainer.getPublishRemotingTransporters());
        initGlobalService();
        nettyRemotingClient.start();
        try{
            ServiceRegistryManager.publishServiceListToRegistry(registryAddress,nettyRemotingClient);
        }catch (Exception e){
            logger.error("publish service to registry failed [{}]",e.getMessage());
        }
        int _port=this.exposePort;
        if(_port!=0){
            this.serverConfig.setListenPort(exposePort);
            this.nettyRemotingServer.start();
            int vipPort=_port-2;
            this.serverConfig.setListenPort(vipPort);
            this.nettyRemotingVipServer.start();
        }

        logger.info("########### provider start successfully.....######");

        if(monitorAddress!=null){
            initMonitorChannel();
        }
    }

    private void initGlobalService(){
        List<RemotingTransporter> list=ProviderServiceContainer.getPublishRemotingTransporters();
        if(null!=list && list.isEmpty()){
            for(RemotingTransporter remotingTransporter:list){
                PublishServiceCustomBody customBody= (PublishServiceCustomBody) remotingTransporter.getCommonHeader();
                String serviceName=customBody.getServiceProviderName();
                ProviderServiceContainer.insertIntoGlobalPublishService(serviceName, customBody);
            }
        }
    }

    public void initMonitorChannel() throws InterruptedException{
        monitorChannel=this.connectionToMonitor();
    }

    public void checkMonitorChannel() throws InterruptedException{
        if((null==monitorChannel||!monitorChannel.isActive())&&monitorAddress==null){
            initMonitorChannel();
        }
    }

    private Channel connectionToMonitor() throws InterruptedException{
        return this.nettyRemotingClient.createChannel(monitorAddress);
    }

    public NettyRemotingClient getNettyRemotingClient(){
        return nettyRemotingClient;
    }

    public String getRegistryAddress(){
        return registryAddress;
    }

    public void setRegistryAddress(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public int getExposePort() {
        return exposePort;
    }

    public void setExposePort(int exposePort) {
        this.exposePort = exposePort;
    }

    public boolean isProviderStateIsHealthy(){
        return ProviderStateIsHealthy;
    }

    public void setProviderStateIsHealthy(boolean providerStateIsHealthy){
        ProviderStateIsHealthy=providerStateIsHealthy;
    }

    public Channel getMonitorChannel(){
        return monitorChannel;
    }

    public void setMonitorChannel(Channel monitorChannel){
        this.monitorChannel=monitorChannel;
    }

    public String getMonitorAddress() {
        return monitorAddress;
    }

    public void setMonitorAddress(String monitorAddress) {
        this.monitorAddress = monitorAddress;
    }




}
