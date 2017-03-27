package com.apin.rpc;

import com.apin.rpc.model.RemotingTransporter;
import io.netty.channel.ChannelHandlerContext;

/**
 * 具体的业务逻辑处理
 */
public interface NettyRequestProcessor {

    RemotingTransporter processRequest(ChannelHandlerContext ctx,RemotingTransporter request) throws Exception;

}
