package com.apin.provider.container;

import com.apin.common.transport.body.PublishServiceCustomBody;
import com.apin.common.utils.Pair;
import com.apin.provider.wrapper.ServiceWrapper;
import com.apin.rpc.model.RemotingTransporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Administrator on 2017/3/17.
 */
public class ProviderServiceContainer {

    //服务及其对应的编织信息及状态
    private static final ConcurrentMap<String,Pair<CurrentServiceState,ServiceWrapper>> serviceProviders=new ConcurrentHashMap<String,Pair<CurrentServiceState,ServiceWrapper>>();

    //要发布的全局信息
    private static final ConcurrentMap<String,PublishServiceCustomBody> globalPublishService=new ConcurrentHashMap<String, PublishServiceCustomBody>();

    //要发布的服务信息
    private static final List<RemotingTransporter>  publishRemotingTransporters= Collections.synchronizedList(new ArrayList<RemotingTransporter>());

    //注册服务及其编织信息和状态
    public static void registerService(String serviceName, ServiceWrapper serviceWrapper) {
        Pair<CurrentServiceState,ServiceWrapper> pair=new Pair<CurrentServiceState, ServiceWrapper>();
        pair.setKey(new CurrentServiceState());
        pair.setValue(serviceWrapper);
        serviceProviders.put(serviceName,pair);
    }

    //根据服务名查询编织信息和状态
    public static Pair<CurrentServiceState, ServiceWrapper> lookupService(String serviceName) {
        return serviceProviders.get(serviceName);
    }

    //查询需要自动降级的服务
    public static List<Pair<String, CurrentServiceState>> getNeedAutoDegradeService() {
        List<Pair<String,CurrentServiceState>> list=new ArrayList<Pair<String,CurrentServiceState>>();
        for(String  serviceName:serviceProviders.keySet()){
            Pair<CurrentServiceState,ServiceWrapper> pair=serviceProviders.get(serviceName);
            if(pair!=null&& pair.getKey().getIsAutoDegrade().get()){
                Pair<String,CurrentServiceState> targetPair=new Pair<String,CurrentServiceState>();
                targetPair.setKey(serviceName);
                targetPair.setValue(pair.getKey());
                list.add(targetPair);
            }
        }
        return list;
    }

    //初始化发布的编织服务信息
    public static void initPublishRemotingTransporters(List<RemotingTransporter> list){
        publishRemotingTransporters.clear();
        for(int i=0;i<list.size();i++){
            list.add(list.get(i));
        }
    }

    //获取发布的编织服务信息,采用了复制的方式，只是为了防止对其进行修改
    public static List<RemotingTransporter> getPublishRemotingTransporters(){
        List<RemotingTransporter> list=new ArrayList<RemotingTransporter>();
        for(int i=0;i<publishRemotingTransporters.size();i++){
            list.add(publishRemotingTransporters.get(i));
        }
        return list;
    }

    //添加服务名及其发布实体
    public  static void insertIntoGlobalPublishService(String serviceName,PublishServiceCustomBody publishServiceCustomBody){
        globalPublishService.put(serviceName,publishServiceCustomBody);
    }

    //获取全局发布信息实体
    public static ConcurrentMap<String,PublishServiceCustomBody> getGlobalPublishService(){
        return globalPublishService;
    }



}
