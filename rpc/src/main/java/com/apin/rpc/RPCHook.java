package com.apin.rpc;

import com.apin.rpc.model.RemotingTransporter;

/**
 * RPC钩子，用于请求发送之前或者接受到应答之后的一些事情
 */
public interface RPCHook {

    void doBeforeRequest(final String remoteAddr,final RemotingTransporter request);

    void doAfterResponse(final String remoteAddr,final RemotingTransporter request,final RemotingTransporter response);
}
