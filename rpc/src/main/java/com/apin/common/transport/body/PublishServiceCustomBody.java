package com.apin.common.transport.body;

import com.apin.common.exception.RemotingCommonCustomException;

/**
 * Created by Administrator on 2017/3/17.
 */
public class PublishServiceCustomBody implements CommonCustomBody{

    private String host;

    private int port;

    private String serviceProviderName;

    private boolean isVipService;

    private boolean isSupportDegradeService;

    private String degradeServicePath;

    private String degradeServiceDesc;

    private volatile int weight;

    private volatile int connCount;

    private long maxCallCountInMinute;

    private boolean isFlowController;

    public void checkFields() throws RemotingCommonCustomException {

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

    public String getServiceProviderName() {
        return serviceProviderName;
    }

    public void setServiceProviderName(String serviceProviderName) {
        this.serviceProviderName = serviceProviderName;
    }

    public boolean isVipService() {
        return isVipService;
    }

    public void setIsVipService(boolean isVipService) {
        this.isVipService = isVipService;
    }

    public boolean isSupportDegradeService() {
        return isSupportDegradeService;
    }

    public void setIsSupportDegradeService(boolean isSupportDegradeService) {
        this.isSupportDegradeService = isSupportDegradeService;
    }

    public String getDegradeServicePath() {
        return degradeServicePath;
    }

    public void setDegradeServicePath(String degradeServicePath) {
        this.degradeServicePath = degradeServicePath;
    }

    public String getDegradeServiceDesc() {
        return degradeServiceDesc;
    }

    public void setDegradeServiceDesc(String degradeServiceDesc) {
        this.degradeServiceDesc = degradeServiceDesc;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getConnCount() {
        return connCount;
    }

    public void setConnCount(int connCount) {
        this.connCount = connCount;
    }

    public long getMaxCallCountInMinute() {
        return maxCallCountInMinute;
    }

    public void setMaxCallCountInMinute(long maxCallCountInMinute) {
        this.maxCallCountInMinute = maxCallCountInMinute;
    }

    public boolean isFlowController() {
        return isFlowController;
    }

    public void setIsFlowController(boolean isFlowController) {
        this.isFlowController = isFlowController;
    }
}
