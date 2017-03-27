package com.apin.provider.model;

import com.apin.common.utils.SystemClock;
import com.apin.rpc.model.RemotingTransporter;

/**
 * Created by Administrator on 2017/3/20.
 */
public class MessageNonAck {

    private final long id;

    private final RemotingTransporter msg;

    private final String address;

    private final long timestamp= SystemClock.millisClock().now();

    public MessageNonAck(RemotingTransporter msg,String address){
        this.msg=msg;
        this.address=address;
        id=msg.getOpaque();
    }

    public long getId(){
        return id;
    }

    public RemotingTransporter getMsg(){
        return msg;
    }

    public String getAddress(){
        return address;
    }


    public long getTimestamp(){
        return timestamp;
    }
}
