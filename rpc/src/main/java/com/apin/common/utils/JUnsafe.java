package com.apin.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Created by Administrator on 2017/3/21.
 */
@SuppressWarnings("all")
public class JUnsafe {

    protected static final Logger logger= LoggerFactory.getLogger(JUnsafe.class);

    private static final Unsafe UNSAFE;

    static{
        Unsafe unsafe;
        try{
            Field unsafeField=Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe= (Unsafe) unsafeField.get(null);
        }catch (Throwable t){
            logger.warn("sun.misc.Unsafe.theUnsafe:unvailabed,{}.",t);
            unsafe=null;
        }
        UNSAFE=unsafe;
    }

    public static Unsafe getUnsafe(){
        return UNSAFE;
    }

    public static ClassLoader getSystemClassLoader(){
        if(System.getSecurityManager()==null){
            return ClassLoader.getSystemClassLoader();
        }else{
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return ClassLoader.getSystemClassLoader();
                }
            });
        }
    }

    public static void throwException(Throwable t){
        if(UNSAFE!=null){
            UNSAFE.throwException(t);
        }else{
            JUnsafe.<RuntimeException>throwException0(t);
        }
    }

    public static <E extends Throwable> void throwException0(Throwable t) throws E{
        throw (E) t;
    }

    private JUnsafe(){}

}
