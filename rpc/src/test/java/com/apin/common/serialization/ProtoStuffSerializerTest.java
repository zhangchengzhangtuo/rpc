package com.apin.common.serialization;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Administrator on 2017/3/9.
 */
public class ProtoStuffSerializerTest {

    @Test
    public void test(){
        ProtoStuffSerializer protoStuffSerializer=new ProtoStuffSerializer();
        long beginTime=System.currentTimeMillis();
        for(long i=0;i<100000;i++){
            ComplexTestObj complexTestObj = new ComplexTestObj("attr1", 2);
            TestCommonCustomBody commonCustomHeader = new TestCommonCustomBody(1, "test",complexTestObj);
            byte[] bytes = protoStuffSerializer.writeObject(commonCustomHeader);

            TestCommonCustomBody body = protoStuffSerializer.readObject(bytes, TestCommonCustomBody.class);
        }

        long endTime=System.currentTimeMillis();
        System.out.println("protoStuff duration: "+(endTime-beginTime)+" ms");
    }
}