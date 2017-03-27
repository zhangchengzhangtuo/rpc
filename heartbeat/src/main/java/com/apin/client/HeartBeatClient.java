package com.apin.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;

import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2017/3/15.
 */
public class HeartBeatClient {

    protected final HashedWheelTimer timer =new HashedWheelTimer();

    private Bootstrap boot;

    private final ConnectorIdleStateTrigger connectorIdleStateTrigger=new ConnectorIdleStateTrigger();

    public void connect(int port,String host) throws Exception{
        EventLoopGroup group=new NioEventLoopGroup();
        boot=new Bootstrap();
        boot.group(group).channel(NioSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO));
        final ConnectionWatchdog watchdog=new ConnectionWatchdog(boot,timer,port,host,true) {
            public ChannelHandler[] handlers() {
                return new ChannelHandler[]{
                        this,//连接监控狗，负责重连
                        new IdleStateHandler(0, 4, 0, TimeUnit.SECONDS),//netty自带的监控是否有数据读写的handler
                        connectorIdleStateTrigger,//用于如果没有读写就触发发送心跳包
                        new StringDecoder(),
                        new StringEncoder(),
                        new HeartBeatClientHandler()
                };
            }
        };
        ChannelFuture future;
        try{
            synchronized (boot){
                boot.handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast(watchdog.handlers());
                    }
                });

                future=boot.connect(host,port);
            }
            future.sync();
        }catch (Throwable t){
            throw new Exception("connectors to fail",t);
        }
    }

    public static void main(String [] args) throws Exception{
        int port=8080;
        if(args!=null && args.length>0){
            try{
                port=Integer.valueOf(args[0]);
            }catch (NumberFormatException e){

            }
        }
        new HeartBeatClient().connect(port,"127.0.0.1");
    }

}
