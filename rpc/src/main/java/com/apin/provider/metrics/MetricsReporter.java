package com.apin.provider.metrics;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/3/21.
 */
public class MetricsReporter implements Serializable{
    private static final long serialVersionUID = 5120051971364564658L;

    private String host;

    private int port;

    private String serviceName;

    private Long callCount=0l;

    private Long failCount=0l;

    private Long requestSize=0l;

    private Long totalRequestTime=0l;


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

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Long getCallCount() {
        return callCount;
    }

    public void setCallCount(Long callCount) {
        this.callCount = callCount;
    }

    public Long getFailCount() {
        return failCount;
    }

    public void setFailCount(Long failCount) {
        this.failCount = failCount;
    }

    public Long getRequestSize() {
        return requestSize;
    }

    public void setRequestSize(Long requestSize) {
        this.requestSize = requestSize;
    }

    public Long getTotalRequestTime() {
        return totalRequestTime;
    }

    public void setTotalRequestTime(Long totalRequestTime) {
        this.totalRequestTime = totalRequestTime;
    }
}
