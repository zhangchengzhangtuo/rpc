package com.apin.common.exception;

/**
 * Created by Administrator on 2017/3/9.
 */
public class RemotingTimeoutException extends RemotingException{

    private static final long serialVersionUID = -1659282008167027190L;

    public RemotingTimeoutException(String message){
        super(message);
    }

    public RemotingTimeoutException(String addr,long timeoutMs){
        this(addr,timeoutMs,null);
    }

    public RemotingTimeoutException(String addr,long timeoutMs,Throwable cause){
        super("wait response on the channel <"+addr+"> timeout,"+timeoutMs+"(ms)",cause);
    }
}
