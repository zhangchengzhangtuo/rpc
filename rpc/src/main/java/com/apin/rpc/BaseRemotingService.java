package com.apin.rpc;

/**
 * netty RPC 模块基本几个流程
 */
public interface BaseRemotingService {

    void init();

    void start();

    void shutdown();

    void registerRPCHook(RPCHook rpcHook);
}
