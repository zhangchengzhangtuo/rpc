package com.apin.provider.manager;

import com.apin.common.exception.RemotingException;
import com.apin.common.serialization.SerializerHolder;
import com.apin.common.transport.body.AckCustomBody;
import com.apin.provider.DefaultProvider;
import com.apin.provider.container.ProviderServiceContainer;
import com.apin.provider.model.MessageNonAck;
import com.apin.rpc.client.NettyRemotingClient;
import com.apin.rpc.model.RemotingTransporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2017/3/17.
 */
public class ServiceRegistryManager {

    private static final Logger logger= LoggerFactory.getLogger(ServiceRegistryManager.class);

    private static final ConcurrentHashMap<Long,MessageNonAck>  messageNonAcks=new ConcurrentHashMap<Long, MessageNonAck>();

    /**
     * 将多个服务发送给多个注册中心
     * @throws InterruptedException
     * @throws RemotingException
     */
    public static void publishServiceListToRegistry(String registryAddress,NettyRemotingClient nettyRemotingClient) throws InterruptedException,RemotingException{
        List<RemotingTransporter> transporterList= ProviderServiceContainer.getPublishRemotingTransporters();
        if(null==transporterList||transporterList.isEmpty()){
            logger.warn("service is empty please call DefaultProvider #publishService method");
            return;
        }

        if(registryAddress==null){
            logger.warn("registry center address is empty please check your address");
            return;
        }

        String [] addresses=registryAddress.split(",");
        if(null!=addresses&&addresses.length>0){
            for(String eachAddress:addresses){
                for(RemotingTransporter request:transporterList){
                    publishServiceToRegistry(request,eachAddress,nettyRemotingClient);
                }
            }
        }
    }

    /**
     * 将某个服务发送给某个注册中心
     * @param request
     * @param eachAddress
     * @throws InterruptedException
     * @throws RemotingException
     */
    private static void publishServiceToRegistry(RemotingTransporter request,String eachAddress,NettyRemotingClient nettyRemotingClient) throws InterruptedException,RemotingException{
        logger.info("[{}] transporters matched", request);
        messageNonAcks.put(request.getOpaque(), new MessageNonAck(request, eachAddress));
        RemotingTransporter remotingTransporter=nettyRemotingClient.invokeSync(eachAddress,request,3000);
        if(null!=remotingTransporter){
            AckCustomBody ackCustomBody= SerializerHolder.serializerImpl().readObject(remotingTransporter.getBytes(),AckCustomBody.class);
            logger.info("received ack info [{}]",ackCustomBody);
            if(ackCustomBody.isSuccess()){
                messageNonAcks.remove(ackCustomBody.getRequestId());
            }
        }else{
            logger.warn("registry center handler timeout");
        }
    }

    public static void checkPublishFailMessage(NettyRemotingClient nettyRemotingClient) throws InterruptedException,RemotingException {
        if(messageNonAcks.keySet()!=null&&messageNonAcks.keySet().size()>0){
            logger.warn("have [{}] message send failed,send again",messageNonAcks.keySet().size());
            for(MessageNonAck ack:messageNonAcks.values()){
                publishServiceToRegistry(ack.getMsg(),ack.getAddress(),nettyRemotingClient);
            }
        }
    }

}
