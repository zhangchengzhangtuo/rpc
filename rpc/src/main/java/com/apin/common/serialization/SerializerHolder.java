package com.apin.common.serialization;

/**
 * Created by Administrator on 2017/3/8.
 */
public class SerializerHolder {

    private static final Serializer serializer=BaseServiceLoader.load(Serializer.class);

    public static Serializer serializerImpl(){
        return serializer;
    }
}
