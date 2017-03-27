package com.apin.rpc.code;

import com.apin.common.exception.RemotingContextException;
import com.apin.common.protocol.RPCProtocol;
import com.apin.rpc.model.RemotingTransporter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
public class RemotingTransporterDecoder extends ReplayingDecoder<State> {

    private static final Logger logger= LoggerFactory.getLogger(RemotingTransporterDecoder.class);

    private static final int MAX_BODY_SIZE=1024*1024*5;

    private final RPCProtocol header=new RPCProtocol();

    public RemotingTransporterDecoder(){
        super(State.HEADER_MAGIC);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch(state()){
            case HEADER_MAGIC:
                checkMagic(in.readShort());
                checkpoint(State.HEADER_TYPE);
            case HEADER_TYPE:
                header.setType(in.readByte());
                checkpoint(State.HEADER_SIGN);
            case HEADER_SIGN:
                header.setSign(in.readByte());
                checkpoint(State.HEADER_ID);
            case HEADER_ID:
                header.setId(in.readLong());
                checkpoint(State.HEADER_BODY_LENGTH);
            case HEADER_BODY_LENGTH:
                header.setBodyLength(in.readInt());
                checkpoint(State.BODY);
            case BODY:
                int bodyLength=checkBodyLength(header.getBodyLength());
                byte [] bytes=new byte[bodyLength];
                in.readBytes(bytes);
                out.add(RemotingTransporter.newInstance(header.getId(),header.getSign(),header.getType(),bytes));
                break;
            default:
                break;
        }
        checkpoint(State.HEADER_MAGIC);
    }

    private int checkBodyLength(int bodyLength) throws RemotingContextException{
        if(bodyLength>MAX_BODY_SIZE){
            throw new RemotingContextException("body of request is bigger than limit value "+MAX_BODY_SIZE);
        }
        return bodyLength;
    }

    private void checkMagic(short magic) throws RemotingContextException{
        if(RPCProtocol.MAGIC!=magic){
            logger.error("Magic is not match");
            throw new RemotingContextException("magic value is not equal "+RPCProtocol.MAGIC);
        }
    }
}
