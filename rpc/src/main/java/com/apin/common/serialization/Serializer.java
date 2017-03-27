package com.apin.common.serialization;

/**
 * Created by Administrator on 2017/3/8.
 */
public interface Serializer {

    <T> byte[] writeObject(T obj);

    <T> T readObject(byte[]  bytes,Class<T> clazz);
}
