package com.apin.rpc.server;

import com.apin.common.exception.RemotingSendRequestException;
import com.apin.common.exception.RemotingTimeoutException;
import com.apin.common.utils.Pair;
import com.apin.rpc.BaseRemotingService;
import com.apin.rpc.NettyChannelInactiveProcessor;
import com.apin.rpc.NettyRequestProcessor;
import com.apin.rpc.model.RemotingTransporter;
import io.netty.channel.Channel;

import java.util.concurrent.ExecutorService;

/**
 * netty服务端另外需要的流程
 */
public interface RemotingServer extends BaseRemotingService {

    RemotingTransporter invokeSync(final Channel channel,final RemotingTransporter request,final long timeoutMs) throws InterruptedException,RemotingSendRequestException,RemotingTimeoutException;

    void registerProcessor(final byte requestCode,final NettyRequestProcessor processor,final ExecutorService executorService);

    void registerChannelInactiveProcessor(final NettyChannelInactiveProcessor processor,final ExecutorService executorService);

    void registerDefaultProcessor(final NettyRequestProcessor processor,final ExecutorService executor);

    Pair<NettyRequestProcessor,ExecutorService> getProcessorPair(final int requestCode);


}
