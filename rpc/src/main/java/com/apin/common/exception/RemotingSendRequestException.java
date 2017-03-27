package com.apin.common.exception;

/**
 * Created by Administrator on 2017/3/9.
 */
public class RemotingSendRequestException extends RemotingException{

    private static final long serialVersionUID = 1328880777383764882L;

    public RemotingSendRequestException(String addr){
        this(addr,null);
    }

    public RemotingSendRequestException(String addr,Throwable cause){
        super("send request to <"+addr+"> failed",cause);
    }
}
