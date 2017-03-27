package com.apin.common.serialization;

import com.apin.common.exception.RemotingCommonCustomException;
import com.apin.common.transport.body.CommonCustomBody;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/3/9.
 */
public class TestCommonCustomBody implements CommonCustomBody,Serializable{

    private static final long serialVersionUID = 2372657311943966714L;

    private int id;

    private String name;

    private ComplexTestObj complexTestObj;

    public TestCommonCustomBody(){

    }

    public TestCommonCustomBody(int id,String name,ComplexTestObj complexTestObj){
        this.name=name;
        this.id=id;
        this.complexTestObj=complexTestObj;
    }

    public void checkFields() throws RemotingCommonCustomException {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ComplexTestObj getComplexTestObj() {
        return complexTestObj;
    }

    public void setComplexTestObj(ComplexTestObj complexTestObj) {
        this.complexTestObj = complexTestObj;
    }

    public String toString(){
        return "TestCommonCustomBody [id="+id+",name="+name+",complexTestObj="+complexTestObj+"]";
    }

}
