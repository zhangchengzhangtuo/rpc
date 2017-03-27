package com.apin.common.serialization;

import java.util.ServiceLoader;

/**
 * Created by Administrator on 2017/3/8.
 */
public class BaseServiceLoader {

    public static <S> S load(Class<S> serviceClass){
        return ServiceLoader.load(serviceClass).iterator().next();
    }


}
