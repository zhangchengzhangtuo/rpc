package com.apin.provider.interceptor;

/**
 * Created by Administrator on 2017/3/22.
 */
public interface ProviderInterceptor {

    void beforeInvoke(String methodName,Object [] args);

    void afterInvoke(String methodName,Object [] args,Object result);
}
