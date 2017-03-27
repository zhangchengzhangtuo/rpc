package com.apin.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2017/3/15.
 */
@ChannelHandler.Sharable
public abstract class ConnectionWatchdog extends ChannelInboundHandlerAdapter implements TimerTask,ChannelHandlerHolder{

    private final Bootstrap bootstrap;

    private final Timer timer;

    private final int port;

    private final String host;

    private volatile boolean reconnect=true;

    private int attempts;

    public ConnectionWatchdog(Bootstrap bootstrap,Timer timer,int port,String host,boolean reconnect){
        this.bootstrap=bootstrap;
        this.timer=timer;
        this.port=port;
        this.host=host;
        this.reconnect=reconnect;
    }

    public void channelActive(ChannelHandlerContext ctx) throws Exception{
        System.out.println("当前链路已经激活了,重连尝试次数重新置为0");
        attempts=0;
        ctx.fireChannelActive();
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception{
        System.out.println("链接关闭");
        if(reconnect){
            System.out.println("链接关闭,将进行重连");
            if(attempts<12){
                attempts++;
                int timeout=2<<attempts;
                timer.newTimeout(this,timeout, TimeUnit.MILLISECONDS);
            }
        }
        ctx.fireChannelInactive();
    }

    public void run(Timeout timeout) throws Exception{
        final ChannelFuture future;
        synchronized (bootstrap){
            bootstrap.handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) throws Exception {
                    channel.pipeline().addLast(handlers());
                }
            });
            future=bootstrap.connect(host,port);
        }
        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                boolean succeed= future.isSuccess();
                if(!succeed){
                    System.out.println("重连失败");
                    future.channel().pipeline().fireChannelInactive();
                }else{
                    System.out.println("重连成功");
                }
            }
        });
    }


}
