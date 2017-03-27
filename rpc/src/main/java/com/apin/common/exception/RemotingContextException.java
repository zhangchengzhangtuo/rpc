package com.apin.common.exception;

/**
 * Created by Administrator on 2017/3/8.
 */
public class RemotingContextException extends RemotingException{
    private static final long serialVersionUID = -3221177740672989786L;

    public RemotingContextException(String message){
        super(message,null);
    }

    public RemotingContextException(String message,Throwable cause){
        super(message,cause);
    }
}
