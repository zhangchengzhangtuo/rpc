package com.apin.common.utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Created by Administrator on 2017/3/9.
 */
public class ConnectionUtils {

    private static final Logger logger= LoggerFactory.getLogger(ConnectionUtils.class);

    public static String parseChannelRemoteAddr(final Channel channel){
        if(null==channel){
            return "";
        }

        final SocketAddress remote=channel.remoteAddress();
        final String addr=remote!=null?remote.toString():"";
        if(addr.length()>0){
            int index=addr.lastIndexOf("/");
            if(index>=0){
                return addr.substring(index+1);
            }
            return addr;
        }

        return "";
    }

    public static Address parseChannelRemoteAddress(final Channel channel){
        String address =parseChannelRemoteAddr(channel);
        if("".equals(address)){
            return null;
        }

        String [] strs=address.split(":");
        return new Address(strs[0],Integer.parseInt(strs[1]));
    }

    public static SocketAddress string2SocketAddress(String addr){
        String [] s=addr.split(":");
        InetSocketAddress isa=new InetSocketAddress(s[0],Integer.valueOf(s[1]));
        return isa;
    }

    public static void closeChannel(Channel channel){
        final String addrRemote=parseChannelRemoteAddr(channel);
        channel.close().addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                logger.info("closeChannel:close the connection to remote address [{}] result:{}",addrRemote,channelFuture.isSuccess());
            }
        });
    }

    public static String exceptionSimpleDesc(Exception e){
        StringBuffer sb=new StringBuffer();
        if(e!=null){
            sb.append(e.toString());

            StackTraceElement [] stackTraceElements=e.getStackTrace();
            if(stackTraceElements!=null && stackTraceElements.length>0){
                StackTraceElement element=stackTraceElements[0];
                sb.append(",");
                sb.append(element.toString());
            }
        }

        return sb.toString();
    }

    public static int getPortFromAddress(String address){
        int port=0;
        if(null!=address){
            String [] ipAndPort=address.split(":");
            if(ipAndPort.length==2){
                port=Integer.parseInt(ipAndPort[1]);
            }
        }
        return port;
    }

}
