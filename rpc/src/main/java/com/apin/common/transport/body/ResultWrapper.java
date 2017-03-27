package com.apin.common.transport.body;

/**
 * Created by Administrator on 2017/3/21.
 */
public class ResultWrapper {

    private Object result;

    private String error;

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
