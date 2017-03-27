package com.apin.common.transport.body;

import com.apin.common.exception.RemotingCommonCustomException;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Administrator on 2017/3/20.
 */
public class RequestCustomBody implements CommonCustomBody{

    private static final AtomicLong invokeIdGenerator=new AtomicLong(0);

    private final long invokeId;

    private String serviceName;

    private Object [] args;

    private long timestamp;

    public RequestCustomBody(){
        this(invokeIdGenerator.getAndIncrement());
    }

    public RequestCustomBody(long invokeId){
        this.invokeId=invokeId;
    }


    public void checkFields() throws RemotingCommonCustomException {

    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
