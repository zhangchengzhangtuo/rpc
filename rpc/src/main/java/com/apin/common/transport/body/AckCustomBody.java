package com.apin.common.transport.body;

import com.apin.common.exception.RemotingCommonCustomException;

/**
 * Created by Administrator on 2017/3/18.
 */
public class AckCustomBody implements CommonCustomBody{

    private long requestId;

    private boolean success;

    public AckCustomBody(long requestId,boolean success){
        this.requestId=requestId;
        this.success=success;
    }

    public String toString(){
        return "AckCustomBody [requestId="+requestId+",success="+success+"]";
    }

    public void checkFields() throws RemotingCommonCustomException {

    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
