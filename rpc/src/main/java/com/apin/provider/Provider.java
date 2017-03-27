package com.apin.provider;

import com.apin.common.exception.RemotingException;
import com.apin.rpc.model.RemotingTransporter;
import io.netty.channel.Channel;

/**
 * 服务提供端需要提供以下接口
 * 1)需要暴露哪些服务[必要]
 * 2)暴露的服务在哪个端口上提供[必要]
 * 3)设置注册中心的地址[必要]
 * 4)暴露启动服务提供者的方法[必须调用]
 * 5)设置provider端提供的监控地址[非必要]
 */
public interface Provider {

    /**
     * 暴露服务的地址端口
     * @param exposePort
     * @return
     */
    Provider serviceListenPort(int exposePort);

    /**
     * 设置注册中心的地址
     * @param registryAddress
     * @return
     */
    Provider registryAddress(String registryAddress);

    /**
     * 监控中心的地址，不是强依赖，不设置也没关系
     * @param monitorAddress
     * @return
     */
    Provider monitorAddress(String monitorAddress);

    /**
     * 需要暴露的实例
     * @param Object
     * @return
     */
    Provider publishService(Object... Object);

    /**
     * 启动provider的实例
     * @throws InterruptedException
     * @throws RemotingException
     */
    void start() throws InterruptedException,RemotingException;


}






























