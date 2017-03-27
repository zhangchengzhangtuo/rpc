package com.apin.rpc.watcher;

import io.netty.channel.ChannelHandler;

/**
 * Created by Administrator on 2017/3/10.
 */
public interface ChannelHandlerHolder {

    ChannelHandler [] handlers();
}
