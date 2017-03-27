package com.apin.common.utils;

/**
 * Created by Administrator on 2017/3/9.
 */
public class Address {

    private String host;

    private int port;

    public Address(){

    }

    public Address(String host,int port){
        this.host=host;
        this.port=port;
    }

    public boolean equals(Object o){
        if(this==o){
            return true;
        }

        if(o==null||getClass()!=o.getClass()){
            return false;
        }

        Address address=(Address)o;
        return port==address.port&&!(host!=null?!host.equals(address.host):address.host!=null);
    }

    @Override
    public int hashCode(){
        int result=host!=null?host.hashCode():0;
        result=31*result+port;
        return result;
    }


    @Override
    public String toString(){
        return "Address{"
                +"host='"+host+"\'"
                +",port="+port+"}";
    }


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
