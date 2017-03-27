package com.apin.provider.manager;

import com.apin.common.utils.SystemClock;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 描述：这是一个服务流计数器，对某一个服务的调用次数进行统计
 * 实现方式：一个环状的计数器桶，每个桶里面有3个计数器
 */
public class ServiceFlowCounter {

    private static final AtomicLong[] metrices=new AtomicLong[] { new AtomicLong(0),new AtomicLong(0),new AtomicLong(0)};

    public static long incrementAtCurrentMinute(){
        long currentTime= SystemClock.millisClock().now();
        int index= (int) ((currentTime/60000)%3);

        AtomicLong atomicLong=metrices[index];
        return atomicLong.incrementAndGet();
    }

    public static long getCurrentCallCountAtLastMinute(){
        long currentTime=SystemClock.millisClock().now();
        int index= (int) ((currentTime/60000)%3);

        AtomicLong atomicLong=metrices[index];
        return atomicLong.get();
    }

    public static long getLastCallCountAtLastMinute(){
        long currentTime=SystemClock.millisClock().now();
        int index= (int) (((currentTime/60000)-1)%3);
        AtomicLong atomicLong=metrices[index];
        return atomicLong.get();
    }

    public static long getNextMinuteCallCount(){
        long currentTime=SystemClock.millisClock().now();
        int index= (int) (((currentTime/60000)+1)%3);
        AtomicLong atomicLong=metrices[index];
        return atomicLong.get();
    }

    public static void clearNextMinuteCallCount(){
        long currentTime=SystemClock.millisClock().now();
        int index= (int) (((currentTime/60000)+1)%3);
        AtomicLong atomicLong=metrices[index];
        atomicLong.set(0);
    }

}
