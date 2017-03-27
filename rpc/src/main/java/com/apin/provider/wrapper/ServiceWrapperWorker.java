package com.apin.provider.wrapper;

import com.apin.provider.interceptor.ProviderProxyHandler;

import java.util.List;

/**
 * 编织服务的接口
 */
public interface ServiceWrapperWorker {

    ServiceWrapperWorker provider(Object serviceProvider);

    ServiceWrapperWorker provider(ProviderProxyHandler proxyHandler,Object serviceProvider);

    List<ServiceWrapper> create();
}
