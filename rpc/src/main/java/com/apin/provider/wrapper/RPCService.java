package com.apin.provider.wrapper;

import java.lang.annotation.*;

/**
 * 方法注解，用于说明RPC接口服务的某个方法对应的9个属性，用于编织该方法
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({   ElementType.METHOD  })
@Documented
public @interface RPCService {

    //服务名
    public String serviceName() default "";

    //负载访问权重
    public int weight() default 50;

    //负责人姓名
    public String responsibilityName() default "system";

    //单实例连接数，注册中心该参数有效，直连无效
    public int connCount() default 1;

    //是否是VIP服务
    public boolean isVIPService() default false;

    //是否支持降级
    public boolean isSupportDegradeService() default false;

    //如果支持降级，降级服务的路径
    public String degradeServicePath() default "";

    //降级服务的描述
    public String degradeServiceDesc() default "";

    //是否单位时间限流
    public boolean isFlowController() default true;

    //单位时间的最大调用量
    public long maxCallCountInMinute() default 100000;

}
