package com.apin.rpc.model;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2017/3/9.
 */
public class RemotingResponse {

    private volatile RemotingTransporter remotingTransporter;

    private volatile Throwable cause;

    private volatile boolean sendRequestOK=true;

    private final long opaque;

    private final InvokeCallback invokeCallback;

    private final long timeoutMs;

    private final long beginTimestamp=System.currentTimeMillis();
    private final CountDownLatch countDownLatch=new CountDownLatch(1);

    public RemotingResponse(long opaque,long timeoutMs,InvokeCallback invokeCallback){
        this.invokeCallback=invokeCallback;
        this.opaque=opaque;
        this.timeoutMs=timeoutMs;
    }

    public void executeInvokeCallback(){
        if(invokeCallback!=null){
            invokeCallback.operationComplete(this);
        }
    }

    public RemotingTransporter waitResponse() throws InterruptedException{
        this.countDownLatch.await(this.timeoutMs, TimeUnit.MILLISECONDS);
        return this.remotingTransporter;
    }

    public void putResponse(final RemotingTransporter remotingTransporter){
        this.remotingTransporter=remotingTransporter;
        this.countDownLatch.countDown();
    }

    public RemotingTransporter getRemotingTransporter() {
        return remotingTransporter;
    }

    public void setRemotingTransporter(RemotingTransporter remotingTransporter) {
        this.remotingTransporter = remotingTransporter;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public boolean isSendRequestOK() {
        return sendRequestOK;
    }

    public void setSendRequestOK(boolean sendRequestOK) {
        this.sendRequestOK = sendRequestOK;
    }

    public long getOpaque() {
        return opaque;
    }

    public InvokeCallback getInvokeCallback() {
        return invokeCallback;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public long getBeginTimestamp() {
        return beginTimestamp;
    }
}
