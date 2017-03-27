package com.apin.rpc.model;

import com.apin.common.protocol.RPCProtocol;
import com.apin.common.transport.body.CommonCustomBody;

import java.util.concurrent.atomic.AtomicLong;


/**
 * **************************************************************************************************
 *                                          Protocol
 *  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
 *       2   │   1   │    1   │     8     │      4      │
 *  ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
 *           │       │        │           │             │
 *  │  MAGIC   Sign    Status   Invoke Id   Body Length                   Body Content              │
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
public class RemotingTransporter extends ByteHolder{

    private static final AtomicLong requestId=new AtomicLong(0l);

    private byte code;

    private transient CommonCustomBody commonHeader;

    private transient long timestamp;

    private long opaque=requestId.getAndIncrement();

    private byte transporterType;

    protected RemotingTransporter(){
    }

    public static RemotingTransporter createRequestTransporter(byte code,CommonCustomBody commonCustomHeader){
        RemotingTransporter remotingTransporter=new RemotingTransporter();
        remotingTransporter.setCode(code);
        remotingTransporter.commonHeader=commonCustomHeader;
        remotingTransporter.transporterType= RPCProtocol.REQUEST_REMOTING;
        return remotingTransporter;
    }

    public static RemotingTransporter createResponseTransporter(byte code,CommonCustomBody commonCustomHeader,long opaque){
        RemotingTransporter remotingTransporter=new RemotingTransporter();
        remotingTransporter.setCode(code);
        remotingTransporter.commonHeader=commonCustomHeader;
        remotingTransporter.transporterType=RPCProtocol.RESPONSE_REMOTING;
        return remotingTransporter;
    }


    public byte getCode() {
        return code;
    }

    public void setCode(byte code) {
        this.code = code;
    }

    public CommonCustomBody getCommonHeader() {
        return commonHeader;
    }

    public void setCommonHeader(CommonCustomBody commonHeader) {
        this.commonHeader = commonHeader;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte getTransporterType() {
        return transporterType;
    }

    public void setTransporterType(byte transporterType) {
        this.transporterType = transporterType;
    }

    public long getOpaque() {
        return opaque;
    }

    public void setOpaque(long opaque) {
        this.opaque = opaque;
    }


    public static RemotingTransporter newInstance(long id,byte sign,byte type,byte[] bytes){
        RemotingTransporter remotingTransporter=new RemotingTransporter();
        remotingTransporter.setCode(sign);
        remotingTransporter.setTransporterType(type);
        remotingTransporter.setOpaque(id);
        remotingTransporter.setBytes(bytes);
        return remotingTransporter;
    }

    public String toString(){
        return "RemotingTransporter [code="+code+",customHeader="+commonHeader+",timestamp="+timestamp
                +",opaque="+opaque+",transporterType="+transporterType+"]";
    }


}
