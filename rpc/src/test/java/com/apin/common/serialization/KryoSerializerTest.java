package com.apin.common.serialization;


import org.junit.Test;

/**
 * Created by Administrator on 2017/3/9.
 */
public class KryoSerializerTest {


    @Test
    public void test(){

        KryoSerializer kryoSerializer=new KryoSerializer();
        long beginTime=System.currentTimeMillis();
        for(long i=0;i<100000;i++){
            ComplexTestObj complexTestObj = new ComplexTestObj("attr1", 2);
            TestCommonCustomBody commonCustomHeader = new TestCommonCustomBody(1, "test",complexTestObj);
            byte[] bytes = kryoSerializer.writeObject(commonCustomHeader);

            TestCommonCustomBody body = kryoSerializer.readObject(bytes, TestCommonCustomBody.class);
        }

        long endTime=System.currentTimeMillis();
        System.out.println("kryo duration: "+(endTime-beginTime)+" ms");
    }
}