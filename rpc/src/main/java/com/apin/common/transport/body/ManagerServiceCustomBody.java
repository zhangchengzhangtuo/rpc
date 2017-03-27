package com.apin.common.transport.body;

import com.apin.common.exception.RemotingCommonCustomException;

/**
 * Created by Administrator on 2017/3/18.
 */
public class ManagerServiceCustomBody implements CommonCustomBody{

//    private ManagerServiceRequestType

    private String serviceName;




    public void checkFields() throws RemotingCommonCustomException {

    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
