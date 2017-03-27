package com.apin.common.serialization;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * Created by Administrator on 2017/3/8.
 */
public class FastjsonSerializer implements Serializer{

    public <T> byte[] writeObject(T obj) {
//        System.out.println("fastjsonSerializer Serializer");
        return JSON.toJSONBytes(obj, SerializerFeature.SortField);
    }

    public <T> T readObject(byte[] bytes, Class<T> clazz) {
//        System.out.println("fastjsonSerializer Deserializer");
        return JSON.parseObject(bytes,clazz, Feature.SortFeidFastMatch);
    }
}

//kryo duration: 4320 ms
//protoStuff duration: 620 ms
//fastjson duration: 1689 ms
