package com.apin.common.exception;

/**
 * Created by Administrator on 2017/3/22.
 */
public class RpcWrapperException extends RuntimeException{

    private static final long serialVersionUID = 6940441133767261869L;

    public RpcWrapperException(){

    }
    public RpcWrapperException(String message){
        super(message);
    }

    public RpcWrapperException(String message,Throwable cause){
        super(message,cause);
    }

    @Override
    public synchronized Throwable fillInStackTrace(){
        return this;
    }
}
