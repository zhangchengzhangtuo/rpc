package com.apin.rpc;

import com.apin.common.exception.RemotingSendRequestException;
import com.apin.common.exception.RemotingTimeoutException;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by Administrator on 2017/3/9.
 */
public interface NettyChannelInactiveProcessor {

    void processChannelInactive(ChannelHandlerContext ctx) throws RemotingSendRequestException,RemotingTimeoutException,InterruptedException;
}
