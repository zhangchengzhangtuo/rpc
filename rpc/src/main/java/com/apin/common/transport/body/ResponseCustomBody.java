package com.apin.common.transport.body;

import com.apin.common.exception.RemotingCommonCustomException;
import com.apin.common.utils.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2017/3/21.
 */
public class ResponseCustomBody implements CommonCustomBody{


    private static final Logger logger= LoggerFactory.getLogger(ResponseCustomBody.class);

    private byte status= Status.OK.getValue();

    private ResultWrapper resultWrapper;

    public  ResponseCustomBody(byte status,ResultWrapper resultWrapper){
        this.status=status;
        this.resultWrapper=resultWrapper;
    }

    public byte getStatus(){
        return status;
    }

    public void setStatus(byte status){
        this.status=status;
    }

    public ResultWrapper getResultWrapper() {
        return resultWrapper;
    }

    public void setResultWrapper(ResultWrapper resultWrapper) {
        this.resultWrapper = resultWrapper;
    }

    public void checkFields() throws RemotingCommonCustomException {

    }

    public Object getResult(){
        if(status==Status.OK.getValue()){
            return getResultWrapper().getResult();
        }else{
            logger.warn("get result occor exception [{}]",getResultWrapper().getError());
            return null;
        }
    }


}
