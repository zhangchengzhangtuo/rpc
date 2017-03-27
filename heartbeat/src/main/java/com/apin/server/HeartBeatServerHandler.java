package com.apin.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Created by Administrator on 2017/3/15.
 */
public class HeartBeatServerHandler extends ChannelInboundHandlerAdapter {

    public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception{
        System.out.println("server channelRead..");
        System.out.println(ctx.channel().remoteAddress()+"->Server :"+msg.toString());
    }

    public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause) throws Exception{
        cause.printStackTrace();
        ctx.close();
    }
}
