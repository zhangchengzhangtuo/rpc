package com.apin.rpc.watcher;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * 该模块主要负责重连，一般主要用于netty客户端
 */
@ChannelHandler.Sharable
public abstract class ConnectionWatchdog extends ChannelInboundHandlerAdapter implements TimerTask,ChannelHandlerHolder{

    private static final Logger logger= LoggerFactory.getLogger(ConnectionWatchdog.class);

    private final Bootstrap bootstrap;

    private final Timer timer;

    private boolean firstConnection=true;

    private volatile SocketAddress remoteAddress;

    private volatile boolean reconnect=true;

    private int attempts;

    public ConnectionWatchdog(Bootstrap bootstrap,Timer timer){
        this.bootstrap=bootstrap;
        this.timer=timer;
    }

    public ChannelHandler[] hanglers() {
        return new ChannelHandler[0];
    }

    public void run(Timeout timeout) throws Exception {
        logger.info("进行重连");
        final ChannelFuture future;

        synchronized (bootstrap){
            bootstrap.handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) throws Exception {
                    channel.pipeline().addLast(handlers());
                }
            });
            future=bootstrap.connect(remoteAddress);
        }

        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                boolean succeed= future.isSuccess();
                logger.warn("Reconnect with {},{}",remoteAddress,succeed?"succeed":"failed");

                if(!succeed){
                    future.channel().pipeline().fireChannelActive();
                }
            }
        });
    }

    public void channelActive(ChannelHandlerContext ctx) throws Exception{
        Channel channel=ctx.channel();
        attempts=0;
        firstConnection=true;
        logger.info("Connects with {}", channel);
        ctx.fireChannelActive();
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception{
        logger.info("当前channel inactive 将关闭连接");
        boolean doReconnect=reconnect;
        if(doReconnect){
            if(firstConnection){
                remoteAddress=ctx.channel().remoteAddress();
                firstConnection=false;
            }

            if(attempts<12){
                attempts++;
            }

            long timeout=2<<attempts;
            logger.info("因为channel关闭所以进行重连");
            timer.newTimeout(this,timeout, TimeUnit.MICROSECONDS);
        }

        logger.warn("Disconnects with {},address:{},reconnect:{}.",ctx.channel(),remoteAddress,doReconnect);

        ctx.fireChannelInactive();
    }


    public boolean isReconnect() {
        return reconnect;
    }

    public void setReconnect(boolean reconnect) {
        this.reconnect = reconnect;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public Timer getTimer() {
        return timer;
    }

    public boolean isFirstConnection() {
        return firstConnection;
    }

    public void setFirstConnection(boolean firstConnection) {
        this.firstConnection = firstConnection;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }
}
