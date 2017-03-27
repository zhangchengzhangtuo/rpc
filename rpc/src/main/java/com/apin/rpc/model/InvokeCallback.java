package com.apin.rpc.model;

import com.apin.rpc.model.RemotingResponse;

/**
 * Created by Administrator on 2017/3/9.
 */
public interface InvokeCallback {

    void operationComplete(final RemotingResponse remotingResponse);
}
