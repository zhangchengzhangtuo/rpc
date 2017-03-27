package com.apin.common.utils;

/**
 * Created by Administrator on 2017/3/10.
 */
public class EpollNativeSupport {

    private static final boolean SUPPORT_NATIVE_ET;

    static {
        boolean epoll;
        try{
            Class.forName("io.netty.channel.epoll.Native");
            epoll=true;
        }catch (Throwable throwable){
            epoll=false;
        }
        SUPPORT_NATIVE_ET=epoll;
    }

    public static boolean isSupportNativeET(){
        return SUPPORT_NATIVE_ET;
    }


}
