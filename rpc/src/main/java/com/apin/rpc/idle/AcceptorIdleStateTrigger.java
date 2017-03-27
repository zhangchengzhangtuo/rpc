package com.apin.rpc.idle;

import com.apin.common.exception.RemotingNoSighException;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * netty服务端检测该链接是否处于空闲状态，是否需要做一些操作，这个地方是抛出异常，然后在异常处理链接那里关闭连接
 */

@ChannelHandler.Sharable
public class AcceptorIdleStateTrigger extends ChannelInboundHandlerAdapter{

    public void userEventTrigger(ChannelHandlerContext ctx,Object evt) throws Exception{
        System.out.println("accept heartbeat");
        if(evt instanceof IdleStateEvent){
            IdleState state =((IdleStateEvent)evt).state();
            if(state==IdleState.READER_IDLE){
                throw new RemotingNoSighException("no sign");
            }else{
                super.userEventTriggered(ctx,evt);
            }
        }
    }

}
