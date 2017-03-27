package com.apin.common.exception;

/**
 * Created by Administrator on 2017/3/8.
 */
public class RemotingCommonCustomException extends RemotingException{

    private static final long serialVersionUID = -544938863512703095L;

    public RemotingCommonCustomException(String message){
        super(message,null);
    }

    public RemotingCommonCustomException(String message,Throwable cause){
        super(message,cause);
    }
}
