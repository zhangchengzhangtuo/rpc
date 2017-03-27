package com.apin.common.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Administrator on 2017/3/11.
 */
public class SystemClock {

    private static final SystemClock millisClock=new SystemClock(1);

    private final long precision;
    private final AtomicLong now;


    public static SystemClock millisClock(){
        return millisClock;
    }

    private SystemClock(long precision){
        this.precision=precision;
        now=new AtomicLong(System.currentTimeMillis());
        scheduleClockUpdating();
    }

    private void scheduleClockUpdating(){
        ScheduledExecutorService scheduledExecutorService= Executors.newSingleThreadScheduledExecutor(new ThreadFactory(){

            public Thread newThread(Runnable r) {
                Thread t=new Thread(r,"system.clock");
                t.setDaemon(true);
                return t;
            }
        });

        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                now.set(System.currentTimeMillis());
            }
        },precision,precision, TimeUnit.MILLISECONDS);
    }

    public long now(){
        return now.get();
    }

    public long precision(){
        return precision;
    }

}
