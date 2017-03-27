package com.apin.provider.wrapper;

import java.util.List;

import static com.apin.common.utils.Constants.*;

/**
 * 编织类，某个provider类方法的说明类，provider将自己提供的服务接口包装成该类发布到注册中心
 */
public class ServiceWrapper {

    /**
     * 原生类
     */
    private Object serviceProvider;

    /**
     * 降级类，默认入参和方法名与原生类一样
     */
    private Object mockDegradeServiceProvider;

    /**
     * 服务名
     */
    private String serviceName;

    /**
     * 该系统的负责人
     */
    private String responsiblityName;

    /**
     * 该类中的方法名
     */
    private String methodName;

    /**
     * 该方法的入参
     */
    private List<Class<?>[]> parameters;

    /**
     * 该方法是否可以降级
     */
    private boolean isSupportDegradeService;

    /**
     * 降级方法的路径
     */
    private String degradeServicePath;

    /**
     * 降级方法的描述
     */
    private String degradeServiceDesc;

    /**
     * 是否是VIP服务
     */
    private boolean isVipService;

    /**
     * 权重，默认是50
     */
    private volatile int weight= DEFAULT_WEIGHT;

    /**
     * 连接数，默认是1个
     */
    private volatile int connCount=DEFAULT_CONNECTION_COUNT;

    /**
     * 单位时间内最大的调用次数
     */
    private long maxCallCountInMinute=DEFAULT_MAX_CALLCOUN_TINMINUTE;

    private boolean isFlowController;



    public ServiceWrapper(Object serviceProvider,Object mockDegradeServiceProvider,String serviceName,String responsiblityName,String methodName,
                          List<Class<?>[]> paramters,boolean isSupportDegradeService,String degradeServicePath,String degradeServiceDesc,int weight,
                          int connCount,boolean isVipService,boolean isFlowController,long maxCallCountInMinute){
        this.serviceProvider=serviceProvider;
        this.mockDegradeServiceProvider=mockDegradeServiceProvider;
        this.serviceName=serviceName;
        this.responsiblityName=responsiblityName;
        this.methodName=methodName;
        this.parameters=paramters;
        this.isSupportDegradeService=isSupportDegradeService;
        this.degradeServicePath=degradeServicePath;
        this.degradeServiceDesc=degradeServiceDesc;
        this.weight=weight;
        this.connCount=connCount;
        this.isVipService=isVipService;
        this.isFlowController=isFlowController;
        this.maxCallCountInMinute=maxCallCountInMinute;
    }


    public Object getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(Object serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public Object getMockDegradeServiceProvider() {
        return mockDegradeServiceProvider;
    }

    public void setMockDegradeServiceProvider(Object mockDegradeServiceProvider) {
        this.mockDegradeServiceProvider = mockDegradeServiceProvider;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getResponsiblityName() {
        return responsiblityName;
    }

    public void setResponsiblityName(String responsiblityName) {
        this.responsiblityName = responsiblityName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
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

    public boolean isVipService() {
        return isVipService;
    }

    public void setIsVipService(boolean isVipService) {
        this.isVipService = isVipService;
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

    public boolean isFlowController() {
        return isFlowController;
    }

    public void setIsFlowController(boolean isFlowController) {
        this.isFlowController = isFlowController;
    }

    public long getMaxCallCountInMinute() {
        return maxCallCountInMinute;
    }

    public void setMaxCallCountInMinute(long maxCallCountInMinute) {
        this.maxCallCountInMinute = maxCallCountInMinute;
    }

    public List<Class<?>[]> getParameters() {
        return parameters;
    }

    public void setParameters(List<Class<?>[]> parameters) {
        this.parameters = parameters;
    }
}
