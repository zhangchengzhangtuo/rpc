package com.apin.provider.manager;

import com.apin.common.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 流控制的集中方式：
 * 1.通过限制单位时间段内调用量来限流
 * 2.通过限制系统的并发调用程度来限流
 * 3.使用漏桶(Leaky Bucket)算法来进行限流
 * 4.使用令牌桶(Token Bucket)算法来进行限流
 *
 */
public class ServiceFlowManager {

    private static final Logger logger= LoggerFactory.getLogger(ServiceFlowManager.class);

    private static final ConcurrentMap<String,Pair<Long,ServiceFlowCounter>> globalFlowControllerMap=new ConcurrentHashMap<String, Pair<Long, ServiceFlowCounter>>();

    /**
     * 设置某个服务的单位时间的最大调用次数
     * @param serviceName
     * @param maxCallCount
     */
    public static void setServiceLimitVal(String serviceName,Long maxCallCount){
        Pair<Long,ServiceFlowCounter> pair=new Pair<Long,ServiceFlowCounter>();
        pair.setKey(maxCallCount);
        pair.setValue(new ServiceFlowCounter());
        globalFlowControllerMap.put(serviceName, pair);
    }

    /**
     * 原子增加某个服务的调用次数
     * @param serviceName
     */
    public static void incrementCallCount(String serviceName){
        Pair<Long,ServiceFlowCounter> pair=globalFlowControllerMap.get(serviceName);
        if(null==pair){
            logger.warn("serviceName [{}] matched no flowController",serviceName);
            return ;
        }

        ServiceFlowCounter serviceFlowCounter =pair.getValue();
        serviceFlowCounter.incrementAtCurrentMinute();
    }

    /**
     * 查看某个服务是否调用
     * @param serviceName
     * @return
     */
    public static boolean isAllow(String serviceName){
        Pair<Long,ServiceFlowCounter> pair=globalFlowControllerMap.get(serviceName);
        if(null==pair){
            logger.warn("ServiceName [{}] matched no flowController",serviceName);
            return false;
        }

        ServiceFlowCounter serviceFlowCounter =pair.getValue();
        Long maxCallCount=pair.getKey();
        long hasCallCount= serviceFlowCounter.getCurrentCallCountAtLastMinute();
        return hasCallCount > maxCallCount ? false:true;

    }

    /**
     * 获取到某一个服务的上一分钟的调用次数
     * @param serviceName
     * @return
     */
    public static Long getLastMinuteCallCount(String serviceName){
        Pair<Long,ServiceFlowCounter> pair=globalFlowControllerMap.get(serviceName);

        if(null==pair){
            logger.warn("serviceName [{}] method no flowController",serviceName);
            return 0l;
        }

        ServiceFlowCounter serviceFlowCounter =pair.getValue();
        return serviceFlowCounter.getLastCallCountAtLastMinute();
    }

    /**
     * 将下一秒的调用次数置为0
     */
    public static void clearAllServiceNextMinuteCallCount(){

        for(String service:globalFlowControllerMap.keySet()){
            Pair<Long,ServiceFlowCounter> pair=globalFlowControllerMap.get(service);
            if(null==pair){
                logger.warn("serviceName [{}] matched no flowController",service);
                continue;
            }
            ServiceFlowCounter serviceFlowCounter =pair.getValue();
            serviceFlowCounter.clearNextMinuteCallCount();
        }
    }

}




















