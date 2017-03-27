package com.apin.client;

import io.netty.channel.ChannelHandler;

/**
 * Created by Administrator on 2017/3/15.
 */
public interface ChannelHandlerHolder {

    ChannelHandler [] handlers();
}
