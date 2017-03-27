package com.apin.common.protocol;

/**
 * **************************************************************************************************
 *                                          Protocol
 *  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
 *       2   │   1   │    1   │     8     │      4      │
 *  ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
 *           │       │        │           │             │
 *  │  MAGIC   Type     Sign    Invoke Id   Body Length                   Body Content              │
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
public class RPCProtocol {

    public static final int HEAD_LENGTH=16;

    public static final short MAGIC=(short)0xbabe;

    public static final byte REQUEST_REMOTING=1;

    public static final byte RESPONSE_REMOTING=2;

    public static final byte RPC_REMOTING=3;



    public static final byte HANDLER_ERROR=-1;

    public static final byte HANDLER_BUSY=-2;



    public static final byte PUBLISH_SERVICE=65;

    public static final byte SUBSCRIBE_SERVICE=69;

    public static final byte MANAGER_SERVICE=70;

    public static final byte DEGRADE_SERVICE=73;

    public static final byte AUTO_DEGRADE_SERVICE=74;

    public static final byte MERTRICS_SERVICE=77;



    public static final byte SUBCRIBE_SERVICE_CANCEL=67;

    public static final byte PUBLISH_SERVICE_CANCEL=68;

    public static final byte SUBCRIBE_RESULT=66;

    public static final byte RPC_REQUEST=72;

    public static final byte RPC_RESPONSE=75;

    public static final byte CHANGE_LOADBALANCE=76;

    public static final byte HEARTBEAT=127;

    public static final byte ACK=126;

    public static final byte COMPRESS=80;

    public static final byte UNCOMPRESS=81;


    private byte type;

    private byte sign;

    private long id;

    private int bodyLength;

//    private byte compress;


    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte getSign() {
        return sign;
    }

    public void setSign(byte sign) {
        this.sign = sign;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getBodyLength() {
        return bodyLength;
    }

    public void setBodyLength(int bodyLength) {
        this.bodyLength = bodyLength;
    }

//    public byte getCompress() {
//        return compress;
//    }
//
//    public void setCompress(byte compress) {
//        this.compress = compress;
//    }
}
