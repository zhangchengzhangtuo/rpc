package com.apin.common.exception;

/**
 * Created by Administrator on 2017/3/8.
 */
public class RemotingException extends Exception{

    private static final long serialVersionUID = 7578817780453366946L;

    public RemotingException(String message){
        super(message);
    }

    public RemotingException(String message,Throwable cause){
        super(message,cause);
    }
}
