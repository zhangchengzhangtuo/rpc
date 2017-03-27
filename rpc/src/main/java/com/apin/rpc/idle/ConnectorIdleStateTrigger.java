package com.apin.rpc.idle;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * netty客户端检测这条连接是否处于空闲状态，如果是的话，需要发送心跳包
 */
public class ConnectorIdleStateTrigger extends ChannelInboundHandlerAdapter{

    public void userEventTriggered(ChannelHandlerContext ctx,Object evt) throws Exception{
        if(evt instanceof IdleStateEvent){
            IdleState state=((IdleStateEvent) evt).state();
            if(state==IdleState.WRITER_IDLE){
                ctx.writeAndFlush(Heartbeats.heartbeatContent());
            }
        }else {
            super.userEventTriggered(ctx,evt);
        }
    }
}
