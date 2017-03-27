package com.apin.rpc.model;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

/**
 * Created by Administrator on 2017/3/10.
 */
public class ChannelWrapper {

    private final ChannelFuture channelFuture;

    public ChannelWrapper(ChannelFuture channelFutue){
        this.channelFuture=channelFutue;
    }

    public boolean isOk(){
        return (this.channelFuture.channel()!=null && this.channelFuture.channel().isActive());
    }

    public boolean isWriteable(){
        return (this.channelFuture.channel().isWritable());
    }

    public Channel getChannel(){
        return this.channelFuture.channel();
    }


    public ChannelFuture getChannelFuture(){
        return channelFuture;
    }

}
