package com.apin.provider.manager;

import com.apin.common.protocol.RPCProtocol;
import com.apin.common.transport.body.ProviderMetricsCustomBody;
import com.apin.common.transport.body.PublishServiceCustomBody;
import com.apin.provider.DefaultProvider;
import com.apin.provider.container.ProviderServiceContainer;
import com.apin.provider.metrics.Meter;
import com.apin.provider.metrics.ServiceMeterManager;
import com.apin.provider.metrics.MetricsReporter;
import com.apin.rpc.model.RemotingTransporter;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Administrator on 2017/3/17.
 */
public class ServiceMonitorManager {

    private static final Logger logger= LoggerFactory.getLogger(ServiceMonitorManager.class);

    public static void sendMetricsInfo(String address,Channel monitorChannel){
        logger.info("scheduled sendMetricsInfos");
        if(address==null){
            logger.warn("monitor address is empty");
            return;
        }

        if(ProviderServiceContainer.getGlobalPublishService()==null){
            logger.warn("publish info is empty");
            return;
        }

        ConcurrentMap<String,Meter> metricsMap= ServiceMeterManager.getGlobalMeterManager();
        if(metricsMap!=null&&metricsMap.keySet()!=null&&metricsMap.values()!=null){
            List<MetricsReporter> reporters=new ArrayList<MetricsReporter>();
            List<Meter> meters=new ArrayList<Meter>();
            meters.addAll(metricsMap.values());
            if(!meters.isEmpty()){
                for(int i=0;i<meters.size();i++){
                    MetricsReporter metricsReporter=new MetricsReporter();
                    String serviceName=meters.get(i).getServiceName();
                    PublishServiceCustomBody body=ProviderServiceContainer.getGlobalPublishService().get(serviceName);
                    if(body==null){
                        logger.warn("servicename [{}] has no publishInfo",serviceName);
                        continue;
                    }
                    metricsReporter.setServiceName(serviceName);
                    metricsReporter.setHost(body.getHost());
                    metricsReporter.setPort(body.isVipService() ? (body.getPort() - 2) : body.getPort());
                    metricsReporter.setCallCount(meters.get(i).getCallCount().get());
                    metricsReporter.setFailCount(meters.get(i).getFailedCount().get());
                    metricsReporter.setTotalRequestTime(meters.get(i).getTotalRequestSize().get());
                    metricsReporter.setRequestSize(meters.get(i).getTotalRequestSize().get());
                    reporters.add(metricsReporter);
                }
                ProviderMetricsCustomBody  body=new ProviderMetricsCustomBody();
                body.setMetricsReporterList(reporters);
                RemotingTransporter remotingTransprter= RemotingTransporter.createRequestTransporter(RPCProtocol.MERTRICS_SERVICE,body);
                if(null!=monitorChannel&&monitorChannel.isActive()&&monitorChannel.isWritable()){
                    monitorChannel.writeAndFlush(remotingTransprter);
                }
            }
        }
    }


}
