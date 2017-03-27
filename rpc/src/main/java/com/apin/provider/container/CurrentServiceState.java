package com.apin.provider.container;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Administrator on 2017/3/18.
 */
public class CurrentServiceState {

    private AtomicBoolean hasDegrade=new AtomicBoolean(false);
    private AtomicBoolean hasLimitStream=new AtomicBoolean(true);
    private AtomicBoolean isAutoDegrade=new AtomicBoolean(false);
    private Integer minSuccessRate=90;


    public AtomicBoolean getHasDegrade() {
        return hasDegrade;
    }

    public void setHasDegrade(AtomicBoolean hasDegrade) {
        this.hasDegrade = hasDegrade;
    }

    public AtomicBoolean getHasLimitStream() {
        return hasLimitStream;
    }

    public void setHasLimitStream(AtomicBoolean hasLimitStream) {
        this.hasLimitStream = hasLimitStream;
    }

    public AtomicBoolean getIsAutoDegrade() {
        return isAutoDegrade;
    }

    public void setIsAutoDegrade(AtomicBoolean isAutoDegrade) {
        this.isAutoDegrade = isAutoDegrade;
    }

    public Integer getMinSuccessRate() {
        return minSuccessRate;
    }

    public void setMinSuccessRate(Integer minSuccessRate) {
        this.minSuccessRate = minSuccessRate;
    }
}
