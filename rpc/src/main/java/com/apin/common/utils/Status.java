package com.apin.common.utils;

/**
 * Created by Administrator on 2017/3/20.
 */
    public enum Status {

    OK(                 (byte)0x20,"OK"),
    CLIENT_ERROR(       (byte)0x30,"CLIENT_ERROR"),
    CLIENT_TIMEOUT(     (byte)0x31,"CLIENT_TIMEOUT"),
    SERVER_TIMEOUT(     (byte)0x32,"SERVER_TIMEOUT"),
    BAD_REQUEST(        (byte)0x40,"BAD_REQUEST"),
    SERVICE_NOT_FOUND(  (byte)0x44,"SERVICE_NOT_FOUND"),
    SERVER_ERROR(       (byte)0x50,"SERVER_ERROR"),
    SERVICE_ERROR(      (byte)0x52,"SERVICE_ERROR"),
    APP_FLOW_CONTROL(   (byte)0x53,"APP_FLOW_CONTROL"),
    PROVIDER_FLOW_CONTROL((byte)0x54,"PROVIDER_FLOW_CONTROL");


    private byte value;

    private String description;

    Status(byte value,String description){
        this.value=value;
        this.description=description;
    }

    public byte getValue() {
        return value;
    }

    public void setValue(byte value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static Status parse(byte value){
        for(Status s : values()){
            if(s.value==value){
                return s;
            }
        }
        return null;
    }

    @Override
    public String toString(){
        return description;
    }
}
