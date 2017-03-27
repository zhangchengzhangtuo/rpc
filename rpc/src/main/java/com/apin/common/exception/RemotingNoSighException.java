package com.apin.common.exception;

/**
 * Created by Administrator on 2017/3/14.
 */
public class RemotingNoSighException extends RemotingException {

    private static final long serialVersionUID = -3138324704905340485L;

    public RemotingNoSighException(String message){
        super(message,null);
    }

    public RemotingNoSighException(String message,Throwable cause){
        super(message,cause);
    }

}
