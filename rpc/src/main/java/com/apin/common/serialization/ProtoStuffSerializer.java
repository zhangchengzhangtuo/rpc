package com.apin.common.serialization;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;

import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2017/3/8.
 */
public class ProtoStuffSerializer implements Serializer{

    private static Map<Class<?>,Schema<?>> cachedSchema=new ConcurrentHashMap<Class<?>,Schema<?>>();

    private static Objenesis objenesis=new ObjenesisStd(true);

    @SuppressWarnings("unchecked")
    public <T> byte[] writeObject(T obj) {
//        System.out.println("ProtoStuffSerializer Serializer");
        Class<T> clazz=(Class<T>)obj.getClass();
        LinkedBuffer buffer= LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try{
            Schema<T> schema=getSchema(clazz);
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage(),e);
        }finally {
            buffer.clear();
        }
    }

    public <T> T readObject(byte[] bytes, Class<T> clazz) {
        try{
            T message=objenesis.newInstance(clazz);
            Schema<T> schema=getSchema(clazz);
            ProtostuffIOUtil.mergeFrom(bytes,message,schema);
            return message;
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage(),e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Schema<T> getSchema(Class<T> clazz){
        Schema<T> schema=(Schema<T>)cachedSchema.get(clazz);
        if(schema==null){
            schema= RuntimeSchema.createFrom(clazz);
            cachedSchema.put(clazz,schema);
        }
        return schema;
    }

}
