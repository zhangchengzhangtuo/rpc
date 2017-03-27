package com.apin.common.transport.body;

import com.apin.common.exception.RemotingCommonCustomException;
import com.apin.provider.metrics.MetricsReporter;

import java.util.List;

/**
 * Created by Administrator on 2017/3/21.
 */
public class ProviderMetricsCustomBody implements CommonCustomBody{

    private List<MetricsReporter> metricsReporterList;

    public void checkFields() throws RemotingCommonCustomException {

    }

    public List<MetricsReporter> getMetricsReporterList() {
        return metricsReporterList;
    }

    public void setMetricsReporterList(List<MetricsReporter> metricsReporterList) {
        this.metricsReporterList = metricsReporterList;
    }
}
