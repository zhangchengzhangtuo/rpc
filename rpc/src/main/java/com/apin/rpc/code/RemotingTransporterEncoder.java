package com.apin.rpc.code;

import com.apin.common.protocol.RPCProtocol;
import com.apin.common.serialization.SerializerHolder;
import com.apin.rpc.model.RemotingTransporter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * **************************************************************************************************
 *                                          Protocol
 *  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
 *       2   │   1   │    1   │     8     │      4      │
 *  ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
 *           │       │        │           │             │
 *  │  MAGIC   Type    Sign    Invoke Id   Body Length                   Body Content              │
 *           │       │        │           │             │
 *  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 *
 * 消息头16个字节定长
 * = 2 // MAGIC = (short) 0xbabe
 * + 1 // 消息标志位, 用来表示消息类型
 * + 1 // 空
 * + 8 // 消息 id long 类型
 * + 4 // 消息体body长度, int类型
 */
public class RemotingTransporterEncoder extends MessageToByteEncoder<RemotingTransporter> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RemotingTransporter msg, ByteBuf out) throws Exception {
        doEncodeRemotingTransporter(msg,out);
    }

    private void doEncodeRemotingTransporter(RemotingTransporter msg,ByteBuf out){
        byte [] body= SerializerHolder.serializerImpl().writeObject(msg.getCommonHeader());
        out.writeShort(RPCProtocol.MAGIC);
        out.writeByte(msg.getTransporterType());
        out.writeByte(msg.getCode());
        out.writeLong(msg.getOpaque());
        out.writeInt(body.length);
        out.writeBytes(body);
    }
}
