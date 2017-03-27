package com.apin.common.transport.body;

import com.apin.common.exception.RemotingCommonCustomException;

/**
 * Created by Administrator on 2017/3/8.
 */
public interface CommonCustomBody {

    void checkFields() throws RemotingCommonCustomException;
}
