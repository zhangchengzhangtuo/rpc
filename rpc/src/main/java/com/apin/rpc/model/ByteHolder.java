package com.apin.rpc.model;

/**
 * Created by Administrator on 2017/3/7.
 */
public class ByteHolder {

    private transient byte [] bytes;


    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public int size(){
        return bytes==null?0:bytes.length;
    }
}
