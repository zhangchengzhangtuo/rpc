package com.apin.provider.processor;

import com.apin.common.protocol.RPCProtocol;
import com.apin.common.serialization.SerializerHolder;
import com.apin.common.transport.body.RequestCustomBody;
import com.apin.common.transport.body.ResponseCustomBody;
import com.apin.common.transport.body.ResultWrapper;
import com.apin.common.utils.*;
import com.apin.provider.DefaultProvider;
import com.apin.provider.container.CurrentServiceState;
import com.apin.provider.container.ProviderServiceContainer;
import com.apin.provider.manager.ServiceFlowManager;
import com.apin.provider.metrics.ServiceMeterManager;
import com.apin.provider.wrapper.ServiceWrapper;
import com.apin.rpc.NettyRequestProcessor;
import com.apin.rpc.model.RemotingTransporter;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by Administrator on 2017/3/17.
 */
public class ProviderRPCServerProcessor implements NettyRequestProcessor{

    private static final Logger logger= LoggerFactory.getLogger(ProviderRPCServerProcessor.class);

    private DefaultProvider defaultProvider;

    public ProviderRPCServerProcessor(DefaultProvider defaultProvider){
        this.defaultProvider=defaultProvider;
    }

    public RemotingTransporter processRequest(ChannelHandlerContext ctx, RemotingTransporter request) throws Exception {
        if(logger.isDebugEnabled()){
            logger.debug("receive request,{} {} {}",request.getCode(), ConnectionUtils.parseChannelRemoteAddr(ctx.channel()),request);
        }

        if(request.getCode()== RPCProtocol.RPC_REQUEST){
            this.handlerRPCRequest(request, ctx.channel());
        }

        return null;
    }


    public void handlerRPCRequest(RemotingTransporter request,Channel channel){
        String serviceName=null;
        RequestCustomBody body=null;
        int requestSize=0;
        //做数据统计
        try{
            byte [] bytes=request.getBytes();
            requestSize=bytes.length;
            request.setBytes(null);
            body= SerializerHolder.serializerImpl().readObject(bytes,RequestCustomBody.class);
            request.setCommonHeader(body);
            serviceName=body.getServiceName();
            ServiceMeterManager.incrementRequestSize(serviceName, requestSize);
            ServiceMeterManager.incrementCallTimes(serviceName);
        }catch (Exception e){
            rejected(Status.BAD_REQUEST,channel,request,serviceName);
            return;
        }

        //查找服务是否存在
        final Pair<CurrentServiceState,ServiceWrapper> pair= ProviderServiceContainer.lookupService(serviceName);
        if(pair==null||pair.getValue()==null){
            rejected(Status.SERVICE_NOT_FOUND,channel,request,serviceName);
            return;
        }

        //进行流控制
        if(pair.getValue().isFlowController()){
            if(!ServiceFlowManager.isAllow(serviceName)){
                rejected(Status.APP_FLOW_CONTROL,channel,request,serviceName);
                return;
            }
        }

        process(pair,request,channel,serviceName,body.getTimestamp());
    }

    /**
     * 通过反射生成接口服务的结果，并将结果返回给客户端
     * @param pair
     * @param request
     * @param channel
     * @param serviceName
     * @param beginTime
     */
    private void process(Pair<CurrentServiceState,ServiceWrapper> pair,final RemotingTransporter request,Channel channel,final String serviceName,final long beginTime){
        Object invokeResult=null;
        CurrentServiceState currentServiceState=pair.getKey();
        ServiceWrapper serviceWrapper=pair.getValue();
        Object targetCallObj=serviceWrapper.getServiceProvider();
        Object [] args=((RequestCustomBody)request.getCommonHeader()).getArgs();

        /**
         * 判断服务是否已经被设定为自动降级，如果设定为自动降级并且有它自己的mock类的时候，则将targetCallObject切换到mock方法上来
         */
        if(currentServiceState.getHasDegrade().get()&&serviceWrapper.getMockDegradeServiceProvider()!=null){
            targetCallObj=serviceWrapper.getMockDegradeServiceProvider();
        }
        String methodName=serviceWrapper.getMethodName();
        List<Class<?>[]> parameterTypesList=serviceWrapper.getParameters();

        Class<?>[] parameterTypes= Reflects.findMatchingParameterTypes(parameterTypesList, args);
        invokeResult=Reflects.fastInvoke(targetCallObj,methodName,parameterTypes,args);
        ResultWrapper resultWrapper=new ResultWrapper();
        resultWrapper.setResult(invokeResult);
        ResponseCustomBody body=new ResponseCustomBody(Status.OK.getValue(),resultWrapper);
        final RemotingTransporter response=RemotingTransporter.createResponseTransporter(RPCProtocol.RPC_RESPONSE,body,request.getOpaque());
        channel.writeAndFlush(response).addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                long elapsed= SystemClock.millisClock().now()-beginTime;
                if(future.isSuccess()){
                    ServiceMeterManager.incrementTotalTime(serviceName,elapsed);
                }else{
                    logger.info("request {} get failed response {}",request,response);
                }
            }
        });
    }


    /**
     * 根据不同的错误生成异常结果，并返回给客户端
     * @param status
     * @param channel
     * @param request
     * @param serviceName
     */
    private void rejected(Status status,Channel channel,final RemotingTransporter request,String serviceName){

        if(null!=serviceName){
            ServiceMeterManager.incrementFailTimes(serviceName);
        }

        ResultWrapper resultWrapper=new ResultWrapper();
        switch(status){
            case BAD_REQUEST:
                resultWrapper.setError("bad request");
            case SERVICE_NOT_FOUND:
                resultWrapper.setError(((RequestCustomBody)request.getCommonHeader()).getServiceName()+" no service found");
                break;
            case APP_FLOW_CONTROL:
            case PROVIDER_FLOW_CONTROL:
                resultWrapper.setError("over unit time call limit");
                break;
            default:
                logger.warn("Unexpected status.",status.getDescription());
                return;
        }
        logger.warn("Service rejected:{}.", resultWrapper.getError());

        ResponseCustomBody responseCustomBody=new ResponseCustomBody(status.getValue(),resultWrapper);
        final RemotingTransporter response=RemotingTransporter.createResponseTransporter(RPCProtocol.RPC_RESPONSE,responseCustomBody,request.getOpaque());
        channel.writeAndFlush(response).addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if(future.isSuccess()){
                    logger.info("request error {} get success response {}",request,response);
                }else{
                    logger.info("request error {} get failed response {}",request,response);
                }
            }
        });
    }
}


















