package com.apin.provider.wrapper;

import com.apin.common.protocol.RPCProtocol;
import com.apin.common.transport.body.PublishServiceCustomBody;
import com.apin.rpc.model.RemotingTransporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 *编织服务管理中心，根据对象实例生成编织服务信息并将其组装成可以远程发送的实体
 */
public class LocalServerWrapperManager {

    public static final Logger logger= LoggerFactory.getLogger(LocalServerWrapperManager.class);

    public static List<RemotingTransporter> wrapperRegisterInfo(int port,Object... obj){
        List<RemotingTransporter> remotingTransporters=new ArrayList<RemotingTransporter>();
        if(null!=obj &&obj.length>0){
            for(Object o:obj){
                DefaultServiceWrapperWorker defaultServiceWrapper=new DefaultServiceWrapperWorker();
                List<ServiceWrapper> serviceWrapperList=defaultServiceWrapper.provider(o).create();
                if(null!=serviceWrapperList&&!serviceWrapperList.isEmpty()){
                    for(ServiceWrapper serviceWrapper:serviceWrapperList){
                        PublishServiceCustomBody commonCustomHeader=new PublishServiceCustomBody();
                        commonCustomHeader.setConnCount(serviceWrapper.getConnCount());
                        commonCustomHeader.setDegradeServiceDesc(serviceWrapper.getDegradeServiceDesc());
                        commonCustomHeader.setDegradeServicePath(serviceWrapper.getDegradeServicePath());
                        commonCustomHeader.setPort(port);
                        commonCustomHeader.setServiceProviderName(serviceWrapper.getServiceName());
                        commonCustomHeader.setIsVipService(serviceWrapper.isVipService());
                        commonCustomHeader.setWeight(serviceWrapper.getWeight());
                        commonCustomHeader.setIsSupportDegradeService(serviceWrapper.isSupportDegradeService());
                        commonCustomHeader.setIsFlowController(serviceWrapper.isFlowController());
                        commonCustomHeader.setMaxCallCountInMinute(serviceWrapper.getMaxCallCountInMinute());

                        RemotingTransporter remotingTransporter=RemotingTransporter.createRequestTransporter(RPCProtocol.PUBLISH_SERVICE,commonCustomHeader);
                        remotingTransporters.add(remotingTransporter);
                    }
                }
            }
        }
        return remotingTransporters;
    }
}
























