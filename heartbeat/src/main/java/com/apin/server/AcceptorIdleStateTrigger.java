package com.apin.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * Created by Administrator on 2017/3/15.
 */
@ChannelHandler.Sharable
public class AcceptorIdleStateTrigger extends ChannelInboundHandlerAdapter{

    public void userEventTriggered(ChannelHandlerContext ctx,Object evt) throws Exception{
        if(evt instanceof IdleStateEvent){
            IdleState state=((IdleStateEvent)evt).state();
            if(state==IdleState.READER_IDLE){
                throw new Exception("idle exception");
            }
        }else{
            super.userEventTriggered(ctx,evt);
        }
    }
}
