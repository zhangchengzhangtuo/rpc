package com.apin.provider.processor;

import com.apin.common.exception.RemotingSendRequestException;
import com.apin.common.exception.RemotingTimeoutException;
import com.apin.provider.DefaultProvider;
import com.apin.rpc.NettyChannelInactiveProcessor;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by Administrator on 2017/3/17.
 */
public class ProviderInactiveProcessor implements NettyChannelInactiveProcessor{

    private DefaultProvider defaultProvider;

    public ProviderInactiveProcessor(DefaultProvider defaultProvider){
        this.defaultProvider=defaultProvider;
    }

    public void processChannelInactive(ChannelHandlerContext ctx) throws RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        defaultProvider.setProviderStateIsHealthy(false);
    }
}
