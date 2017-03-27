package com.apin.provider.manager;

import com.apin.common.protocol.RPCProtocol;
import com.apin.common.serialization.SerializerHolder;
import com.apin.common.transport.body.AckCustomBody;
import com.apin.common.transport.body.ManagerServiceCustomBody;
import com.apin.common.transport.body.PublishServiceCustomBody;
import com.apin.common.utils.Pair;
import com.apin.provider.DefaultProvider;
import com.apin.provider.container.CurrentServiceState;
import com.apin.provider.container.ProviderServiceContainer;
import com.apin.provider.metrics.ServiceMeterManager;
import com.apin.provider.wrapper.ServiceWrapper;
import com.apin.rpc.model.RemotingTransporter;
import io.netty.channel.Channel;

import java.util.List;

/**
 * * 描述：
 * 对于RPC而言，服务的降级也是必不可少的何为服务的降级，就是在业务洪流来的时候，服务器的压力陡增，数据库的压力也很大的时候，轻量化服务的功效
 * 比如某个非核心服务需要调用数据库的，我们降级服务不需要调用数据库，就比如我们在某某电商购物的时候，商品详情页的侧边栏一般会有电商推荐的
 * 一些比较类似的产品，这个后台的机制可能是某个推荐算法，根据用户浏览商品的记录给出推荐的产品，这是非核心的逻辑，这个功能在服务器的压力比较
 * 大的时候，可以进行降级处理，我们可以给出几个默认的产品返回，因为推荐算法可能会涉及大数据的计算和分析，甚至涉及几次的数据库查询，在这个
 * 时候我们如果让这个后台方法默认返回几个固定的值的时候，可以减轻服务的压力，给其他的核心服务，例如支付，详情页等服务做出服务资源的让步
 *
 * 实现：
 * 1)在这个地方每次通过反射调用服务接口的时候，首先判断是否是降级服务，如果是降级服务的话，会被路由到该服务的降级接口，因此这个地方需要维护一张表
 * 用来记录服务及其状态（是否是降级服务）,
 * 2)另一个就是运行中的服务降级的实现，有两种方法，一种是人工降级，另一种是自动降级，自动降级需要设定一个评价标准，当达到这个标准之后将服务的状态
 * 设为降级服务，这个地方需要定时任务定时去检测是否达到这个标准，
 * 3)评价标准的设定：本Demo的标准是服务接口的调用成功率低于最低成功率
 */
public class ServiceDegradeManager {

    /**
     * 检查符合自动降级的服务
     */
    public static void checkAutoDegrade(){
        //获取到所有需要降级的服务名
        List<Pair<String,CurrentServiceState>> needDegradeServices= ProviderServiceContainer.getNeedAutoDegradeService();

        //如果当前实例需要降级的服务列表不为空的情况下，循环每个列表
        if(!needDegradeServices.isEmpty()){
            for(Pair<String,CurrentServiceState> pair:needDegradeServices){
                String serviceName=pair.getKey();
                Integer minSuccessRate=pair.getValue().getMinSuccessRate();
                Integer realSuccessRate= ServiceMeterManager.calcServiceSuccessRate(serviceName);
                if(minSuccessRate>realSuccessRate){
                    final Pair<CurrentServiceState,ServiceWrapper> _pair=ProviderServiceContainer.lookupService(serviceName);
                    CurrentServiceState currentServiceState=_pair.getKey();
                    if(!currentServiceState.getHasDegrade().get()){
                        currentServiceState.getHasDegrade().set(true);
                    }
                }
            }
        }

    }

}
