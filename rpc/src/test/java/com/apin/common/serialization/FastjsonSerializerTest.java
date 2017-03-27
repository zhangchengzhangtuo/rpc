package com.apin.common.serialization;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Administrator on 2017/3/9.
 */
public class FastjsonSerializerTest {

    @Test
    public void test(){

        FastjsonSerializer fastjsonSerializer=new FastjsonSerializer();

        long beginTime=System.currentTimeMillis();
        for(long i=0;i<100000;i++){
            ComplexTestObj complexTestObj = new ComplexTestObj("attr1", 2);
            TestCommonCustomBody commonCustomHeader = new TestCommonCustomBody(1, "test",complexTestObj);
            byte[] bytes = fastjsonSerializer.writeObject(commonCustomHeader);

            TestCommonCustomBody body = fastjsonSerializer.readObject(bytes, TestCommonCustomBody.class);
        }

        long endTime=System.currentTimeMillis();
        System.out.println("fastjson duration: "+(endTime-beginTime)+" ms");
    }

}

//kryo duration: 4320 ms
//protoStuff duration: 620 ms
//fastjson duration: 1689 ms