package com.apin.provider.processor;

import com.apin.common.protocol.RPCProtocol;
import com.apin.common.serialization.SerializerHolder;
import com.apin.common.transport.body.AckCustomBody;
import com.apin.common.transport.body.ManagerServiceCustomBody;
import com.apin.common.transport.body.PublishServiceCustomBody;
import com.apin.common.utils.ConnectionUtils;
import com.apin.common.utils.Pair;
import com.apin.provider.container.CurrentServiceState;
import com.apin.provider.container.ProviderServiceContainer;
import com.apin.provider.wrapper.ServiceWrapper;
import com.apin.rpc.NettyRequestProcessor;
import com.apin.rpc.model.RemotingTransporter;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2017/3/17.
 */
public class ProviderDegradeServerProcessor implements NettyRequestProcessor{

    private static final Logger logger= LoggerFactory.getLogger(ProviderDegradeServerProcessor.class);


    public RemotingTransporter processRequest(ChannelHandlerContext ctx, RemotingTransporter request) throws Exception {
        if(logger.isDebugEnabled()){
            logger.debug("receive request,{} {} {}",request.getCode(), ConnectionUtils.parseChannelRemoteAddr(ctx.channel()),request);
        }

        switch(request.getCode()){
            case RPCProtocol.DEGRADE_SERVICE:
                return handlerDegradeServiceRequest(request, ctx.channel(), RPCProtocol.DEGRADE_SERVICE);
            case RPCProtocol.AUTO_DEGRADE_SERVICE:
                return handlerDegradeServiceRequest(request, ctx.channel(), RPCProtocol.AUTO_DEGRADE_SERVICE);
        }

        return null;
    }


    public RemotingTransporter handlerDegradeServiceRequest(RemotingTransporter request,Channel channel,byte degradeService){
        //默认的ack返回体
        AckCustomBody ackCustomBody=new AckCustomBody(request.getOpaque(),false);
        RemotingTransporter remotingTransporter=RemotingTransporter.createRequestTransporter(RPCProtocol.ACK,ackCustomBody);

        //发布服务是空的时候，默认返回操作失败
        if(ProviderServiceContainer.getPublishRemotingTransporters()==null || ProviderServiceContainer.getPublishRemotingTransporters().size()==0){
            return remotingTransporter;
        }

        //请求体
        ManagerServiceCustomBody subscribeRequestCustomBody= SerializerHolder.serializerImpl().readObject(request.getBytes(),ManagerServiceCustomBody.class);
        //服务名
        String serviceName=subscribeRequestCustomBody.getServiceName();

        //判断请求的服务是否在发布的服务中
        boolean checkServiceIsExist=false;
        for(RemotingTransporter eachTranporter : ProviderServiceContainer.getPublishRemotingTransporters()){
            PublishServiceCustomBody body= (PublishServiceCustomBody) eachTranporter.getCommonHeader();
            if(body.getServiceProviderName().equals(serviceName)&&body.isSupportDegradeService()){
                checkServiceIsExist=true;
                break;
            }
        }
        if(checkServiceIsExist){
            final Pair<CurrentServiceState,ServiceWrapper> pair= ProviderServiceContainer.lookupService(serviceName);
            CurrentServiceState currentServiceState=pair.getKey();
            if(degradeService==RPCProtocol.DEGRADE_SERVICE){
                currentServiceState.getHasDegrade().set(!currentServiceState.getHasDegrade().get());
            }else if(degradeService==RPCProtocol.AUTO_DEGRADE_SERVICE){
                currentServiceState.getIsAutoDegrade().set(true);
            }
            ackCustomBody.setSuccess(true);
        }
        return remotingTransporter;
    }
}
