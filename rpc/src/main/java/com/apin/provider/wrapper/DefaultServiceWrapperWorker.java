package com.apin.provider.wrapper;

import com.apin.common.exception.RpcWrapperException;
import com.apin.common.utils.Reflects;
import com.apin.provider.container.ProviderServiceContainer;
import com.apin.provider.manager.ServiceFlowManager;
import com.apin.provider.interceptor.ProviderProxyHandler;
import io.netty.util.internal.StringUtil;
import net.bytebuddy.ByteBuddy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import static net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default.INJECTION;
import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.not;
/**
 * 编织服务的具体类
 */
public class DefaultServiceWrapperWorker implements ServiceWrapperWorker{

    private Logger logger = LoggerFactory.getLogger(DefaultServiceWrapperWorker.class);

    //全局拦截代理
    private volatile ProviderProxyHandler globalProviderProxyHandler;

    private Object serviceProvider;

    private Object mockDegradeServiceProvider;

    /**
     * 将服务提供类封装一下，在该接口服务前后添加拦截器，
     * @param serviceProvider
     * @return
     */
    public ServiceWrapperWorker provider(Object serviceProvider) {
        if(null==globalProviderProxyHandler){
            this.serviceProvider=serviceProvider;
        }else{
            Class<?> globalProxyCls=generateProviderProxyClass(globalProviderProxyHandler,serviceProvider.getClass());
            this.serviceProvider=copyProviderProperties(serviceProvider, Reflects.newInstance(globalProxyCls));
        }
        return this;
    }

    /**
     * 将服务提供类封装一下，在该接口服务前后添加拦截器，
     * @param providerProxyHandler
     * @param serviceProvider
     * @return
     */
    public ServiceWrapperWorker provider(ProviderProxyHandler providerProxyHandler,Object serviceProvider){
        Class<?> proxyCls=generateProviderProxyClass(providerProxyHandler,serviceProvider.getClass());
        if(globalProviderProxyHandler==null){
            this.serviceProvider=copyProviderProperties(serviceProvider,Reflects.newInstance(proxyCls));
        }else{
            Class<?> globalProxyCls=generateProviderProxyClass(globalProviderProxyHandler,proxyCls);
            this.serviceProvider=copyProviderProperties(serviceProvider,Reflects.newInstance(globalProxyCls));
        }
        return this;
    }

    /**
     * 生成代理类Agent
     * @param proxyHandler
     * @param providerCls
     * @param <T>
     * @return
     */
    private <T> Class<? extends T> generateProviderProxyClass(ProviderProxyHandler proxyHandler,Class<T> providerCls){
        try{
            return new ByteBuddy()
                    .subclass(providerCls)
                    .method(isDeclaredBy(providerCls))
                    .intercept(to(proxyHandler,"handler").filter(not(isDeclaredBy(Object.class))))
                    .make()
                    .load(providerCls.getClassLoader(),INJECTION)
                    .getLoaded();
        }catch (Exception e){
            logger.error("Generate proxy [{},handler:{}] fail:{}.",providerCls,proxyHandler,e.getMessage());
            return providerCls;
        }
    }

    /**
     * 将普通服务提供者封装成代理类
     * @param provider
     * @param proxy
     * @param <F>
     * @param <T>
     * @return
     */
    private <F,T> T copyProviderProperties(F provider,T proxy){
        List<String> providerFieldNames=new ArrayList<String>();
        for(Class<?> cls=provider.getClass();cls!=null;cls=cls.getSuperclass()){
            try{
                for(Field f:cls.getDeclaredFields()){
                    providerFieldNames.add(f.getName());
                }
            }catch (Throwable throwable){
            }
        }
        for(String name:providerFieldNames){
            try{
                Reflects.setValue(proxy,name,Reflects.getValue(provider,name));
            }catch (Throwable e){

            }
        }
        return proxy;
    }

    /**
     * 根据对象实例创建编织服务信息，并将其注册到providerRegistryController中
     * @return
     */
    public List<ServiceWrapper> create() {
        List<ServiceWrapper> serviceWrappers=new ArrayList<ServiceWrapper>();
        RPCService rpcService=null;
        for(Class<?> cls=serviceProvider.getClass();cls!=Object.class;cls=cls.getSuperclass()){
            Method[] methods=cls.getMethods();
            if(null!=methods && methods.length>0){
                for(Method method:methods){
                    rpcService =method.getAnnotation(RPCService.class);
                    if(null!=rpcService){
                        //服务名
                        String serviceName= StringUtil.isNullOrEmpty(rpcService.serviceName())?method.getName():rpcService.serviceName();
                        //负责人
                        String responsibilityName=rpcService.responsibilityName();
                        //方法weight
                        Integer weight=rpcService.weight();
                        //连接数，默认是1 一个实例一个连接其实是够用的
                        Integer connCount=rpcService.connCount();
                        //是否支持服务降级
                        boolean isSupportDegradeService=rpcService.isSupportDegradeService();
                        //是否是VIP服务，如果是VIP服务，则默认在port-2的端口暴露方法，与其它的方法使用不同的
                        boolean isVIPService=rpcService.isVIPService();
                        //暴露的降级方法的路径
                        String degradeServicePath=rpcService.degradeServicePath();
                        //降级方法的描述
                        String degradeServiceDesc=rpcService.degradeServiceDesc();
                        //是否进行限流
                        boolean isFlowControl=rpcService.isFlowController();
                        Long maxCallCount=rpcService.maxCallCountInMinute();
                        if(maxCallCount<=0){
                            throw new RpcWrapperException("max call count must over zero at unit time");
                        }
                        ServiceFlowManager.setServiceLimitVal(serviceName,maxCallCount);
                        if(isSupportDegradeService){
                            Class<?> degradeClass=null;
                            try{
                                degradeClass=Class.forName(degradeServicePath);
                                Object nativeObj=degradeClass.newInstance();
                                if(null==globalProviderProxyHandler){
                                    this.mockDegradeServiceProvider=nativeObj;
                                }else{
                                    Class<?> globalProxyCls=generateProviderProxyClass(globalProviderProxyHandler,nativeObj.getClass());
                                    this.mockDegradeServiceProvider=copyProviderProperties(nativeObj,Reflects.newInstance(globalProxyCls));
                                }
                            }catch(Exception e){
                                logger.error("[{}] class can not create by reflect [{}]",degradeServicePath,e.getMessage());
                                throw new RpcWrapperException("degradeService path "+degradeServicePath+" create failed");
                            }
                        }

                        String methodName=method.getName();
                        Class<?> [] classes=method.getParameterTypes();
                        List<Class<?>[]> paramters=new ArrayList<Class<?>[]>();
                        paramters.add(classes);

                        ServiceWrapper serviceWrapper=new ServiceWrapper(serviceProvider,mockDegradeServiceProvider,serviceName,
                                responsibilityName,methodName,paramters,isSupportDegradeService,degradeServicePath,degradeServiceDesc,
                                weight,connCount,isVIPService,isFlowControl,maxCallCount);
                        ProviderServiceContainer.registerService(serviceName, serviceWrapper);
                        serviceWrappers.add(serviceWrapper);
                    }
                }
            }
        }
        return serviceWrappers;
    }

}
