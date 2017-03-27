package com.apin.common.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Administrator on 2017/3/8.
 */
public class KryoSerializer implements Serializer{

    public <T> byte[] writeObject(T obj) {
//        System.out.println("KryoSerializer serializer");
        Kryo kryo=new Kryo();
        kryo.setReferences(false);
        kryo.register(obj.getClass(),new JavaSerializer());

        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        Output output=new Output(baos);
        kryo.writeClassAndObject(output, obj);
        output.flush();
        output.close();

        byte []  b=baos.toByteArray();
        try{
            baos.flush();
            baos.close();
        }catch (IOException e){
            e.printStackTrace();
        }
        return b;
    }

    public <T> T readObject(byte[] bytes, Class<T> clazz) {
//        System.out.println("kryoSerializer deserializer");
        Kryo kryo=new Kryo();
        kryo.setReferences(false);
        kryo.register(clazz,new JavaSerializer());

        ByteArrayInputStream bais=new ByteArrayInputStream(bytes);
        Input input=new Input(bais);
        return (T) kryo.readClassAndObject(input);
    }
}

//kryo duration: 4320 ms
//protoStuff duration: 620 ms
//fastjson duration: 1689 ms
