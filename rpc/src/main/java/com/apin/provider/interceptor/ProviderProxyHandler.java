package com.apin.provider.interceptor;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 */
public class ProviderProxyHandler {

    private static final Logger logger= LoggerFactory.getLogger(ProviderProxyHandler.class);

    private final CopyOnWriteArrayList<ProviderInterceptor> interceptors=new CopyOnWriteArrayList<ProviderInterceptor>();

    public Object invoke(@SuperCall Callable<Object> superMethod,@Origin Method method,@AllArguments @RuntimeType Object [] args) throws Exception{
        String methodName=method.getName();
        for(int i=interceptors.size()-1;i>=0;i--){
            ProviderInterceptor interceptor=interceptors.get(i);
            try{
                interceptor.beforeInvoke(methodName,args);
            }catch (Throwable e){
                logger.warn("Interceptor[{}#beforeInvoke]:{}.",interceptor.getClass().getName());
            }
        }

        Object result=null;
        try{
            result=superMethod.call();
        }finally{
            for(int i=0;i<interceptors.size();i++){
                ProviderInterceptor interceptor=interceptors.get(i);
                try{
                    interceptor.afterInvoke(methodName,args,result);
                }catch (Throwable e){
                    logger.warn("Interceptor[{}#afterInvoke]:{}",interceptor.getClass().getName());
                }
            }
        }
        return result;
    }

    public ProviderProxyHandler withInterceptor(ProviderInterceptor interceptor){
        interceptors.add(interceptor);
        return this;
    }

    public ProviderProxyHandler withInterceptor(ProviderInterceptor... interceptors){
        for(ProviderInterceptor interceptor:interceptors){
            withInterceptor(interceptor);
        }
        return this;
    }
}
