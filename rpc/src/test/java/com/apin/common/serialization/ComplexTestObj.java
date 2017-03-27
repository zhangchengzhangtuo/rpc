package com.apin.common.serialization;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/3/9.
 */
public class ComplexTestObj implements Serializable {


    private static final long serialVersionUID = 1166198356683980277L;

    private String attr1;

    private Integer attr2;

    public ComplexTestObj(){

    }

    public ComplexTestObj(String attr1,Integer attr2){
        super();
        this.attr1=attr1;
        this.attr2=attr2;
    }


    public String getAttr1() {
        return attr1;
    }

    public void setAttr1(String attr1) {
        this.attr1 = attr1;
    }

    public Integer getAttr2() {
        return attr2;
    }

    public void setAttr2(Integer attr2) {
        this.attr2 = attr2;
    }

    @Override
    public String toString(){
        return "ComplexTestObj [attr1="+attr1+",attr2="+attr2+"]";
    }
}
