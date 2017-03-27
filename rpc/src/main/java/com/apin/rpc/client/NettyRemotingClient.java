package com.apin.rpc.client;

import static java.util.concurrent.TimeUnit.SECONDS;
import com.apin.common.exception.RemotingException;
import com.apin.common.exception.RemotingSendRequestException;
import com.apin.common.exception.RemotingTimeoutException;
import com.apin.common.utils.*;
import com.apin.rpc.NettyChannelInactiveProcessor;
import com.apin.rpc.NettyRemotingBase;
import com.apin.rpc.NettyRequestProcessor;
import com.apin.rpc.RPCHook;
import com.apin.rpc.code.RemotingTransporterDecoder;
import com.apin.rpc.code.RemotingTransporterEncoder;
import com.apin.rpc.idle.ConnectorIdleStateTrigger;
import com.apin.rpc.idle.IdleStateChecker;
import com.apin.rpc.model.ChannelWrapper;
import com.apin.rpc.model.RemotingTransporter;
import com.apin.rpc.watcher.ConnectionWatchdog;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * NettyServer 和 NettyClient 由于身份不同所以导致启动方式不同，但是他们的业务处理其实是一样的，也就是都可以接受请求和接受应答
 */
public class NettyRemotingClient extends NettyRemotingBase implements RemotingClient {

    private static final Logger logger= LoggerFactory.getLogger(NettyRemotingClient.class);

    private Bootstrap bootstrap;

    private EventLoopGroup worker;

    private int nWorkers;

    protected volatile ByteBufAllocator allocator;

    private final Lock lockChannelTables=new ReentrantLock();

    private static final long LockTimeoutMs=3000;

    private DefaultEventExecutorGroup defaultEventExecutorGroup;

    private final NettyClientConfig nettyClientConfig;

    private volatile int writeBufferHighWaterMark=-1;
    private volatile int writeBufferLowWaterMark=-1;

    private final ConnectorIdleStateTrigger idleStateTrigger=new ConnectorIdleStateTrigger();

    protected HashedWheelTimer timer=new HashedWheelTimer(new NamedThreadFactory("netty.timer"));

    private RPCHook rpcHook;

    private final ConcurrentHashMap<String,ChannelWrapper> channelTables=new ConcurrentHashMap<String, ChannelWrapper>();

    private boolean isReconnect=true;

    public NettyRemotingClient(NettyClientConfig nettyClientConfig){
        this.nettyClientConfig=nettyClientConfig;
        if(null!=nettyClientConfig){
            nWorkers=nettyClientConfig.getClientWorkerThreads();
            writeBufferLowWaterMark=nettyClientConfig.getWriteBufferLowWaterMark();
            writeBufferHighWaterMark=nettyClientConfig.getWriteBufferHighWaterMark();
        }
        init();
    }

    private boolean isNativeEt(){
        return EpollNativeSupport.isSupportNativeET();
    }

    private EventLoopGroup  initEventLoopGroup(int nWorkers,ThreadFactory workerFactory){
        return isNativeEt()?new EpollEventLoopGroup(nWorkers,workerFactory):new NioEventLoopGroup(nWorkers,workerFactory);
    }

    public void init() {
        ThreadFactory workerFactory=new DefaultThreadFactory("netty.client");
        worker=initEventLoopGroup(nWorkers,workerFactory);
        bootstrap=new Bootstrap().group(worker);

        if(worker instanceof EpollEventLoopGroup){
            ((EpollEventLoopGroup)worker).setIoRatio(100);
        }else if(worker instanceof NioEventLoopGroup){
            ((NioEventLoopGroup)worker).setIoRatio(100);
        }

        bootstrap.option(ChannelOption.ALLOCATOR,allocator).option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT)
                .option(ChannelOption.SO_REUSEADDR,true).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) SECONDS.toMillis(3));

        bootstrap.option(ChannelOption.SO_KEEPALIVE,true).option(ChannelOption.TCP_NODELAY,true).option(ChannelOption.ALLOW_HALF_CLOSURE, false);
        if(writeBufferLowWaterMark>=0&&writeBufferHighWaterMark>0){
            WriteBufferWaterMark waterMark=new WriteBufferWaterMark(writeBufferLowWaterMark,writeBufferHighWaterMark);
            bootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK, waterMark);
        }

    }

    public void start() {
        this.defaultEventExecutorGroup=new DefaultEventExecutorGroup(nettyClientConfig.getClientWorkerThreads(), new ThreadFactory() {

            private AtomicInteger threadIndex=new AtomicInteger(0);

            public Thread newThread(Runnable r) {
                return new Thread(r,"NettyClientWorkerThread_"+this.threadIndex.incrementAndGet());
            }
        });

        if(isNativeEt()){
            bootstrap.channel(EpollSocketChannel.class);
        }else{
            bootstrap.channel(NioSocketChannel.class);
        }

        final ConnectionWatchdog watchdog=new ConnectionWatchdog(bootstrap,timer) {
            public ChannelHandler[] handlers() {
                return new ChannelHandler[]{
                  this,
                  new RemotingTransporterDecoder(),
                  new RemotingTransporterEncoder(),
                  new IdleStateChecker(timer,0, Constants.WRITER_IDLE_TIME_SECONDS,0),
                  idleStateTrigger,
                  new NettyClientHandler()
                };
            }
        };

        watchdog.setReconnect(isReconnect);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline().addLast(defaultEventExecutorGroup, watchdog.hanglers());
            }
        });
    }

    public void shutdown(){
        try{
            this.timer.stop();
            this.timer=null;
            for(ChannelWrapper cw:this.channelTables.values()){
                this.closeChannel(null,cw.getChannel());
            }

            this.channelTables.clear();
            this.worker.shutdownGracefully();
            if(this.defaultEventExecutorGroup!=null){
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        }catch (Exception e){
            logger.error("NettyRemotingClient shutdown exception,",e);
        }
    }

    private void closeChannel(String addr,Channel channel){
        if(null==channel){
            return;
        }

        final String addrRemote=null==addr? ConnectionUtils.parseChannelRemoteAddr(channel):addr;
        try{
            if(this.lockChannelTables.tryLock(LockTimeoutMs, TimeUnit.MILLISECONDS)){
                try{
                    boolean removeItemFromTable=true;
                    final ChannelWrapper prevCW=this.channelTables.get(addrRemote);
                    logger.info("closeChannel:begin close the channel [{}] Found:{}",addrRemote,(prevCW!=null));
                    if(null==prevCW){
                        logger.info("closeChannel:the channel [{}] has been removed from the channel table before",addrRemote);
                        removeItemFromTable=false;
                    }else if(prevCW.getChannel()!=channel){
                        logger.info("closeChannel:the channel [{}] has been closed before,and has been create again,nothing to do.",addrRemote);
                        removeItemFromTable=false;
                    }
                    if(removeItemFromTable){
                        this.channelTables.remove(addrRemote);
                        logger.info("closeChannel:the channel [{}] was removed from channel table",addrRemote);
                    }

                    ConnectionUtils.closeChannel(channel);
                }catch (Exception e){
                    logger.error("closeChannel:close the channel exception",e);
                }finally{
                    this.lockChannelTables.unlock();
                }
            }else{
                logger.warn("closeChannel: try to lock channel table,but timeout, {}ms", LockTimeoutMs);
            }
        }catch (Exception e){
            logger.error("closeChannel exception",e);
        }
    }

    public Channel createChannel(String addr) throws InterruptedException{
        ChannelWrapper cw=this.channelTables.get(addr);
        if(cw!=null && cw.isOk()){
            return cw.getChannel();
        }

        if(this.lockChannelTables.tryLock(LockTimeoutMs,TimeUnit.MILLISECONDS)){
            try{
                boolean createNewConnection=false;
                cw=this.channelTables.get(addr);
                if(cw!=null){
                    if(cw.isOk()){
                        return cw.getChannel();
                    }else if(!cw.getChannelFuture().isDone()){
                        createNewConnection=false;
                    }else{
                        this.channelTables.remove(addr);
                        createNewConnection=true;
                    }
                }else{
                    createNewConnection=true;
                }

                if(createNewConnection){
                    ChannelFuture channelFuture=this.bootstrap.connect(ConnectionUtils.string2SocketAddress(addr));
                    logger.info("createChannel:begin to connect remote host [{}] asynchronously",addr);
                    cw=new ChannelWrapper(channelFuture);
                    this.channelTables.put(addr,cw);
                }
            }catch(Exception e){
                logger.info("createChannel:create channel exception",e);
            }finally {
                this.lockChannelTables.unlock();
            }
        }else{
            logger.warn("createChannel:try to lock channel table,but timeout,{} ms",LockTimeoutMs);
        }

        if(cw!=null){
            ChannelFuture channelFuture=cw.getChannelFuture();
            if(channelFuture.awaitUninterruptibly(this.nettyClientConfig.getConnectTimeoutMs())){
                if(cw.isOk()){
                    logger.info("createChannel:connect remote host [{}] success,{}",addr,channelFuture.toString());
                    return cw.getChannel();
                }else{
                    logger.warn("createChannel: connect remote host["+addr+"] failed,"+channelFuture.toString(),channelFuture.cause());
                }
            }else{
                logger.warn("createChannel:connect remote host [{}] timeout {} ms,{}",addr,this.nettyClientConfig.getConnectTimeoutMs(),channelFuture.toString());
            }
        }
        return null;
    }


    public Channel getAndCreateChannel(final String addr) throws InterruptedException{
        if(null==addr){
            logger.warn("address is null");
            return null;
        }
        ChannelWrapper cw=this.channelTables.get(addr);
        if(cw!=null&&cw.isOk()){
            return cw.getChannel();
        }

        return this.createChannel(addr);
    }



    public void registerProcessor(byte requestCode, NettyRequestProcessor processor, ExecutorService executorService) {
        ExecutorService executorServiceThis=executorService;
        if(null==executorServiceThis){
            executorServiceThis=this.publicExecutor;
        }

        Pair<NettyRequestProcessor,ExecutorService> pair=new Pair<NettyRequestProcessor,ExecutorService>(processor,executorServiceThis);
        this.processorTable.put(requestCode,pair);
    }

    public boolean isChannelWriteable(String addr){
        ChannelWrapper cw=this.channelTables.get(addr);
        if(cw!=null&&cw.isOk()){
            return cw.isWriteable();
        }
        return true;
    }


    public RemotingTransporter invokeSync(String addr, RemotingTransporter request, long timeoutMs) throws RemotingTimeoutException, RemotingSendRequestException, InterruptedException, RemotingException {
        final Channel channel=this.getAndCreateChannel(addr);
        if(channel!=null&&channel.isActive()){
            try{
                if(this.rpcHook!=null){
                    this.rpcHook.doBeforeRequest(addr,request);
                }

                RemotingTransporter response=this.invokeSyncImpl(channel,request,timeoutMs);
                if(this.rpcHook!=null){
                    this.rpcHook.doAfterResponse(ConnectionUtils.parseChannelRemoteAddr(channel),request,response);
                }
                return response;
            }catch (RemotingSendRequestException e){
                logger.warn("invokeSync:send request exception,so close the channel [{}]",addr);
                this.closeChannel(addr,channel);
                throw e;
            }catch(RemotingTimeoutException e){
                logger.warn("invokeSync:wait response timeout exception, the channel[{}]",addr);
                this.closeChannel(addr,channel);
                throw e;
            }
        }else{
            this.closeChannel(addr,channel);
            throw new RemotingException(addr+" connection exception");
        }
    }

    public void registerChannelInactiveProcessor(NettyChannelInactiveProcessor processor,ExecutorService executor) {
        if(null==executor){
            executor=this.publicExecutor;
        }
        this.defaultChannelInactiveProcessor=new Pair<NettyChannelInactiveProcessor, ExecutorService>(processor,executor);
    }

    public void setReconnect(boolean isReconnect) {
        this.isReconnect=isReconnect;
    }



    public void registerRPCHook(RPCHook rpcHook) {
        this.rpcHook=rpcHook;
    }

    protected RPCHook getRPCHook(){
        return rpcHook;
    }

    class NettyClientHandler extends SimpleChannelInboundHandler<RemotingTransporter> {

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, RemotingTransporter remotingTransporter) throws Exception {
            processMessageReceived(channelHandlerContext,remotingTransporter);
        }

        public void channelInactive(ChannelHandlerContext ctx) throws Exception{
            processChannelInactive(ctx);
        }
    }
}
