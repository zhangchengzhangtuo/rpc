package com.apin.rpc;

import com.apin.common.exception.RemotingSendRequestException;
import com.apin.common.exception.RemotingTimeoutException;
import com.apin.common.protocol.RPCProtocol;
import com.apin.common.utils.ConnectionUtils;
import com.apin.common.utils.Pair;
import com.apin.rpc.model.RemotingResponse;
import com.apin.rpc.model.RemotingTransporter;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Administrator on 2017/3/9.
 */
public abstract class NettyRemotingBase {

    private static final Logger logger= LoggerFactory.getLogger(NettyRemotingBase.class);

    protected final ConcurrentHashMap<Long,RemotingResponse> responseTable=new ConcurrentHashMap<Long, RemotingResponse>(256);

    protected Pair<NettyRequestProcessor,ExecutorService> defaultRequestProcessor;

    protected Pair<NettyChannelInactiveProcessor,ExecutorService> defaultChannelInactiveProcessor;

    protected final ExecutorService publicExecutor= Executors.newFixedThreadPool(4, new ThreadFactory() {
        private AtomicInteger threadIndex=new AtomicInteger(0);
        public Thread newThread(Runnable r) {
            return new Thread(r,"NettyClientPublicExecutor_"+this.threadIndex.incrementAndGet());
        }
    });

    protected final HashMap<Byte,Pair<NettyRequestProcessor,ExecutorService>> processorTable=new HashMap<Byte, Pair<NettyRequestProcessor, ExecutorService>>(64);


    /**
     * 异步RPC的核心实现
     * @param channel
     * @param request
     * @param timeoutMs
     * @return
     * @throws RemotingTimeoutException
     * @throws RemotingSendRequestException
     * @throws InterruptedException
     */
    public RemotingTransporter invokeSyncImpl(final Channel channel,final RemotingTransporter request,final long timeoutMs) throws RemotingTimeoutException,RemotingSendRequestException,InterruptedException{
        try{
            final RemotingResponse remotingResponse=new RemotingResponse(request.getOpaque(),timeoutMs,null);
            this.responseTable.put(request.getOpaque(), remotingResponse);
            /**
             * 主要判断请求是否发送成功
             */
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if(channelFuture.isSuccess()){
                        remotingResponse.setSendRequestOK(true);
                        return;
                    }else{
                        remotingResponse.setSendRequestOK(false);
                    }

                    responseTable.remove(request.getOpaque());
                    remotingResponse.setCause(channelFuture.cause());
                    remotingResponse.putResponse(null);
                    logger.warn("use channel [{}] send msg [{}] failed and faile reason is [{}]",channel,request,channelFuture.cause().getMessage());
                }
            });

            /**
             * 等待异步应答
             */
            RemotingTransporter remotingTransporter=remotingResponse.waitResponse();
            if(null==remotingTransporter){
                if(remotingResponse.isSendRequestOK()){
                    throw new RemotingTimeoutException(ConnectionUtils.parseChannelRemoteAddr(channel),timeoutMs,remotingResponse.getCause());
                }else{
                    throw new RemotingSendRequestException(ConnectionUtils.parseChannelRemoteAddr(channel),remotingResponse.getCause());
                }
            }
            return remotingTransporter;
        }finally {
            this.responseTable.remove(request.getOpaque());
        }
    }

    protected void processMessageReceived(ChannelHandlerContext ctx,RemotingTransporter msg){
        if(logger.isDebugEnabled()){
            logger.debug("channel [] received RemotingTransporter is [{}]",ctx.channel(),msg);
        }

        final RemotingTransporter remotingTransporter=msg;

        if(remotingTransporter!=null){
            switch(remotingTransporter.getTransporterType()){
                case RPCProtocol.REQUEST_REMOTING:
                    processRemotingRequest(ctx,remotingTransporter);
                    break;
                case RPCProtocol.RESPONSE_REMOTING:
                    processRemotingResponse(ctx,remotingTransporter);
                    break;
                default:
                    break;

            }
        }
    }

    protected void processChannelInactive(final ChannelHandlerContext ctx){
        final Pair<NettyChannelInactiveProcessor,ExecutorService> pair=this.defaultChannelInactiveProcessor;
        if(pair!=null){
            Runnable runnable=new Runnable() {
                public void run() {
                    try {
                        pair.getKey().processChannelInactive(ctx);
                    } catch (Exception e) {
                        logger.error("server occur exception [{}]",e.getMessage());
                    }
                }
            };

            try{
                pair.getValue().submit(runnable);
            }catch (Exception e){
                logger.error("server is busy,[{}]",e.getMessage());
            }
        }
    }

    protected abstract RPCHook getRPCHook();

    protected void processRemotingRequest(final ChannelHandlerContext ctx,final RemotingTransporter remotingTransporter){
        final Pair<NettyRequestProcessor,ExecutorService> matchedPair=this.processorTable.get(remotingTransporter.getCode());
        final Pair<NettyRequestProcessor,ExecutorService> pair=null==matchedPair?this.defaultRequestProcessor:matchedPair;
        if(pair!=null){
            Runnable run=new Runnable() {
                public void run() {
                    try{
                        RPCHook rpcHook=NettyRemotingBase.this.getRPCHook();
                        if(rpcHook!=null){
                            rpcHook.doBeforeRequest(ConnectionUtils.parseChannelRemoteAddr(ctx.channel()),remotingTransporter);
                        }
//                        在这个地方会调用具体的处理类，即NettyRequestProcessor类的具体实现来处理这个请求
                        final RemotingTransporter response=pair.getKey().processRequest(ctx,remotingTransporter);
                        if(rpcHook!=null){
                            rpcHook.doAfterResponse(ConnectionUtils.parseChannelRemoteAddr(ctx.channel()),remotingTransporter,response);
                        }
                        if(null!=response){
                            ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
                                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                                    if(!channelFuture.isSuccess()){
                                        logger.error("fail to send response,exception is [{}]",channelFuture.cause().getMessage());
                                    }
                                }
                            });
                        }
                    }catch (Exception e){
                        logger.error("processor occur exception [{}]",e.getMessage());
                        final RemotingTransporter response=RemotingTransporter.newInstance(remotingTransporter.getOpaque(),RPCProtocol.RESPONSE_REMOTING,RPCProtocol.HANDLER_ERROR,null);
                        ctx.writeAndFlush(response);
                    }
                }
            };
            try{
                pair.getValue().submit(run);
            }catch (Exception e){
                logger.error("server is busy,[{}]",e.getMessage());
                final RemotingTransporter response=RemotingTransporter.newInstance(remotingTransporter.getOpaque(),RPCProtocol.RESPONSE_REMOTING,RPCProtocol.HANDLER_BUSY,null);
                ctx.writeAndFlush(response);
            }
        }
    }


    protected void processRemotingResponse(ChannelHandlerContext ctx,RemotingTransporter remotingTransporter){
        final RemotingResponse remotingResponse=responseTable.get(remotingTransporter.getOpaque());
        if(null!=remotingResponse){
            remotingResponse.setRemotingTransporter(remotingTransporter);
            remotingResponse.putResponse(remotingTransporter);
            responseTable.remove(remotingTransporter.getOpaque());
        }else{
            logger.warn("received response but matched Id is removed from responseTable maybe timeout");
            logger.warn(remotingTransporter.toString());
        }
    }

}
