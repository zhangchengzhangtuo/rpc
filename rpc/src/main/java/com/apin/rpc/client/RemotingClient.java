package com.apin.rpc.client;

import com.apin.common.exception.RemotingException;
import com.apin.common.exception.RemotingSendRequestException;
import com.apin.common.exception.RemotingTimeoutException;
import com.apin.rpc.BaseRemotingService;
import com.apin.rpc.NettyChannelInactiveProcessor;
import com.apin.rpc.NettyRequestProcessor;
import com.apin.rpc.model.RemotingTransporter;

import java.util.concurrent.ExecutorService;

/**
 * Created by Administrator on 2017/3/9.
 */
public interface RemotingClient extends BaseRemotingService{

    public RemotingTransporter invokeSync(final String addr,final RemotingTransporter request,final long timeoutMs) throws RemotingTimeoutException,RemotingSendRequestException,InterruptedException,RemotingException;

    void registerProcessor(final byte requestCode,final NettyRequestProcessor processor,final ExecutorService executorService);

    void registerChannelInactiveProcessor(NettyChannelInactiveProcessor processor,ExecutorService executor);

    boolean isChannelWriteable(final String addr);

    void setReconnect(boolean isReconnect);

}
