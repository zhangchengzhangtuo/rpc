package com.apin.common.utils;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.sf.cglib.reflect.FastClass;

/**
 * Created by Administrator on 2017/3/20.
 */
public class Reflects {

    private static final Objenesis objenesis=new ObjenesisStd(true);
    private static final ConcurrentMap<Class<?>,FastClass> fastClassCache=new ConcurrentHashMap<Class<?>,FastClass>();

    /**
     * 原生类到包装类的映射Map
     */
    private static final Map<Class<?>,Class<?>> primitiveWrapperMap=new IdentityHashMap<Class<?>,Class<?>>();
    /**
     * primitive 原生类，wrapper 包装类
     */
    static{
        primitiveWrapperMap.put(Boolean.TYPE,Boolean.class);
        primitiveWrapperMap.put(Byte.TYPE,Boolean.class);
        primitiveWrapperMap.put(Character.TYPE,Character.class);
        primitiveWrapperMap.put(Short.TYPE,Short.class);
        primitiveWrapperMap.put(Integer.TYPE,Integer.class);
        primitiveWrapperMap.put(Long.TYPE,Long.class);
        primitiveWrapperMap.put(Double.TYPE,Double.class);
        primitiveWrapperMap.put(Float.TYPE,Float.class);
        primitiveWrapperMap.put(Void.TYPE,Void.TYPE);
    }

    /**
     * 包装类到原生类的映射Map
     */
    private static final Map<Class<?>,Class<?>> wrapperPrimitiveMap=new IdentityHashMap<Class<?>, Class<?>>();

    static{
        for(Map.Entry<Class<?>,Class<?>> entry:primitiveWrapperMap.entrySet()){
            final Class<?> wrapperClass=entry.getValue();
            final Class<?> primitiveClass=entry.getKey();
            if(!primitiveClass.equals(wrapperClass)){
                wrapperPrimitiveMap.put(wrapperClass,primitiveClass);
            }
        }
    }

    /**
     * Array of primitive number types ordered by "promotability"
     */
    private static final Class<?>[] ORDERED_PRIMITIVE_TYPES={
            Byte.TYPE,//一个字节
            Short.TYPE,//两个字节
            Character.TYPE,//两个字节
            Integer.TYPE,//四个字节
            Long.TYPE,//八个字节
            Float.TYPE,//四个字节
            Double.TYPE//八个字节
    };

    /**
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T newInstance(Class<T> clazz){
        return newInstance(clazz,true);
    }

    /**
     * create a new object
     * @param clazz
     * @param constructorCalled
     * @param <T>
     * @return
     */
    public static <T> T newInstance(Class<T> clazz,boolean constructorCalled){
        if(constructorCalled){
            try{
                return clazz.newInstance();
            }catch (Exception e){
                JUnsafe.throwException(e);
            }
        }else{
            return objenesis.newInstance(clazz);
        }
        return null;
    }

    /**
     * JDK动态代理反向生成对象
     * @param obj
     * @param methodName
     * @param parameterTypes
     * @param args
     * @return
     */
    public static Object invoke(Object obj,String methodName,Class<?>[] parameterTypes,Object[] args){
        Object value=null;
        try{
            Method method=obj.getClass().getMethod(methodName,parameterTypes);
            method.setAccessible(true);
            value=method.invoke(obj,args);
        }catch (Exception e){
            JUnsafe.throwException(e);
        }
        return value;
    }

    /**
     * Cglib 动态代理反向生成对象
     * @param obj
     * @param methodName
     * @param parameterTypes
     * @param args
     * @return
     */
    public static Object fastInvoke(Object obj,String methodName,Class<?>[] parameterTypes,Object [] args){
        Class<?> clazz=obj.getClass();
        FastClass fastClass=fastClassCache.get(clazz);
        if(fastClass==null){
            FastClass newFastClass=FastClass.create(clazz);
            fastClass=fastClassCache.putIfAbsent(clazz,newFastClass);
            if(fastClass==null){
                fastClass=newFastClass;
            }
        }
        Object value=null;
        try{
            value=fastClass.invoke(methodName,parameterTypes,obj,args);
        }catch (InvocationTargetException e){
            JUnsafe.throwException(e);
        }

        return value;
    }

    /**
     *获取类的属性
     * @param clazz
     * @param name
     * @return
     * @throws NoSuchFieldException
     */
    public static Field getField(Class<?> clazz,String name) throws NoSuchFieldException{
        for(Class<?> cls=clazz;cls!=null;cls=cls.getSuperclass()){
            try{
                return cls.getDeclaredField(name);
            }catch (Throwable ignored){

            }
        }
        throw new NoSuchFieldException(clazz.getName()+"#"+name);
    }

    /**
     *获取静态属性
     * @param clazz
     * @param name
     * @return
     */
    public static Object getStaticValue(Class<?> clazz,String name){
        Object value=null;
        try{
            Field fd=setAccessible(getField(clazz,name));
            value=fd.get(null);
        }catch (Exception e){
            JUnsafe.throwException(e);
        }
        return value;
    }

    /**
     *设置静态属性
     * @param clazz
     * @param name
     * @param value
     */
    public static void setStaticValue(Class<?> clazz,String name,Object value){
        try{
            Field fd=setAccessible(getField(clazz,name));
            fd.set(null,value);
        }catch (Exception e){
            JUnsafe.throwException(e);
        }
    }

    /**
     *获取某个对象的某个属性值
     * @param o
     * @param name
     * @return
     */
    public static Object getValue(Object o,String name){
        Object value=null;
        try{
            Field fd=setAccessible(getField(o.getClass(),name));
            value=fd.get(o);
        }catch(Exception e){
            JUnsafe.throwException(e);
        }
        return value;
    }

    /**
     *设置某个对象的某个属性的值
     * @param o
     * @param name
     * @param value
     */
    public static void setValue(Object o,String name,Object value){
        try{
            Field fd=setAccessible(getField(o.getClass(),name));
            fd.set(o,value);
        }catch (Exception e){
            JUnsafe.throwException(e);
        }
    }

    /**
     *获取基本类型默认值
     * @param clazz
     * @return
     */
    public static Object getTypeDefaultValue(Class<?> clazz){
        if(clazz.isPrimitive()){
            if(clazz==byte.class){
                return (byte)0;
            }

            if(clazz==short.class){
                return (short)0;
            }

            if(clazz==int.class){
                return (int)0;
            }

            if(clazz==long.class){
                return (long)0;
            }

            if(clazz==float.class){
                return 0F;
            }

            if(clazz==double.class){
                return (char)0;
            }

            if(clazz==boolean.class){
                return false;
            }
        }
        return null;
    }

    /**
     *获取某个对象的简单类名(就是不包括包名)
     * @param o
     * @return
     */
    public static String simpleClassName(Object o){
        return o==null?"null_object":simpleClassName(o.getClass());
    }

    /**
     *获取某个类的简单类名
     * @param clazz
     * @return
     */
    public static String simpleClassName(Class<?> clazz){
        if(clazz==null){
            return "null_class";
        }
        Package pkg=clazz.getPackage();
        return pkg==null?clazz.getName():clazz.getName().substring(pkg.getName().length() + 1);
    }

    /**
     *根据参数找到匹配的类类型
     * @param parameterTypesList
     * @param args
     * @return
     */
    public static Class<?> [] findMatchingParameterTypes(List<Class<?>[]> parameterTypesList,Object [] args){
        if(parameterTypesList.size()==1){
            return parameterTypesList.get(0);
        }

        Class<?>[] parameterTypes;
        if(args==null|| args.length==0){
            parameterTypes=new Class[0];
        }else{
            parameterTypes=new Class[args.length];
            for(int i=0;i<args.length;i++){
                parameterTypes[i]=args[i].getClass();
            }
        }

        Class<?> [] bestMatch=null;
        for(Class<?>[] pTypes:parameterTypesList){
            if(isAssignable(parameterTypes,pTypes,true)){
                if(bestMatch==null||compareParameterTypes(pTypes,bestMatch,parameterTypes)<0){
                    bestMatch=pTypes;
                }
            }
        }
        return bestMatch;
    }

    /**
     *
     * @param classArray
     * @param toClassArray
     * @param autoboxing
     * @return
     */
    public static boolean isAssignable(Class<?>[] classArray,Class<?>[] toClassArray,final boolean autoboxing){
        if(classArray.length!=toClassArray.length){
            return false;
        }

        for(int i=0;i<classArray.length;i++){
            if(!isAssignable(classArray[i],toClassArray[i],autoboxing)){
                return false;
            }
        }
        return true;
    }

    /**
     *是否能将一个类转化为另一个类
     * @param cls
     * @param toClass
     * @param autoboxing
     * @return
     */
    public static boolean isAssignable(Class<?> cls,final Class<?> toClass,final boolean autoboxing){
        if(toClass==null){
            return false;
        }

        if(cls==null){
            return !(toClass.isPrimitive());
        }

        if(autoboxing){
            if(cls.isPrimitive()&&!toClass.isPrimitive()){
                cls=primitiveToWrapper(cls);
                if(cls==null){
                    return false;
                }
            }

            if(toClass.isPrimitive()&&!cls.isPrimitive()){
                cls=wrapperToPrimitve(cls);
                if(cls==null){
                    return false;
                }
            }
        }

        if(cls.equals(toClass)){
            return true;
        }

        if(cls.isPrimitive()){
            if(!toClass.isPrimitive()){
                return false;
            }

            if(Boolean.TYPE.equals(cls)){
                return false;
            }

            if(Integer.TYPE.equals(cls)){
                return Long.TYPE.equals(toClass)|| Float.TYPE.equals(toClass)||Double.TYPE.equals(toClass);
            }

            if(Long.TYPE.equals(cls)){
                return Float.TYPE.equals(toClass)||Double.TYPE.equals(toClass);
            }

            if(Float.TYPE.equals(cls)){
                return Double.TYPE.equals(toClass);
            }

            if(Double.TYPE.equals(cls)){
                return false;
            }

            if(Character.TYPE.equals(cls)){
                return Integer.TYPE.equals(toClass)||Long.TYPE.equals(toClass)||Float.TYPE.equals(toClass)||Double.TYPE.equals(toClass);
            }

            if(Short.TYPE.equals(cls)){
                return Integer.TYPE.equals(toClass)||Long.TYPE.equals(toClass)||Float.TYPE.equals(toClass)||Double.TYPE.equals(toClass);
            }

            if(Byte.TYPE.equals(cls)){
                return Short.TYPE.equals(toClass)||Integer.TYPE.equals(toClass)||Long.TYPE.equals(toClass)||Float.TYPE.equals(toClass)||Double.TYPE.equals(toClass);
            }
            return false;
        }

        return toClass.isAssignableFrom(cls);
    }

    /**
     *原生类转化为包装类
     * @param cls
     * @return
     */
    public static Class<?> primitiveToWrapper(final Class<?> cls){
        Class<?> convertedClass=cls;
        if(cls!=null&&cls.isPrimitive()){
            convertedClass=primitiveWrapperMap.get(cls);
        }
        return convertedClass;
    }

    /**
     * 包装类转化为原始类
     * @param cls
     * @return
     */
    public static Class<?> wrapperToPrimitve(final Class<?> cls){
        return wrapperPrimitiveMap.get(cls);
    }

    public static int compareParameterTypes(Class<?>[] left,Class<?> [] right,Class<?> [] actual){
        final float leftCost=getTotalTransformationCost(actual,left);
        final float rightCost=getTotalTransformationCost(actual,right);
        return leftCost<rightCost?-1:rightCost<leftCost?1:0;
    }

    public static float getTotalTransformationCost(final Class<?>[] srcArgs,final Class<?>[] dstArgs){
        float totalCost=0.0f;
        for(int i=0;i<srcArgs.length;i++){
            Class<?> srcClass,dstClass;
            srcClass=srcArgs[i];
            dstClass=dstArgs[i];
            totalCost+=getObjectTransformationCost(srcClass,dstClass);
        }
        return totalCost;
    }

    public static float getObjectTransformationCost(Class<?> srcClass,final Class<?> dstClass){
        if(dstClass.isPrimitive()){
            return getPrimitivePromotionCost(srcClass,dstClass);
        }
        float cost=0.0f;
        while(srcClass!=null&&!dstClass.equals(srcClass)){
            if(dstClass.isInterface()&&isAssignable(srcClass,dstClass,true)){
                cost+=0.25f;
                break;
            }
            cost++;
            srcClass=srcClass.getSuperclass();
        }

        if(srcClass==null){
            cost+=1.5f;
        }
        return cost;
    }

    private static float getPrimitivePromotionCost(final Class<?> srcClass,final Class<?> dstClass){
        float cost=0.0f;
        Class<?> cls=srcClass;
        if(!cls.isPrimitive()){
            cost+=0.1f;
            cls=wrapperToPrimitve(cls);
        }
        for(int i=0;cls!=dstClass&&i<ORDERED_PRIMITIVE_TYPES.length;i++){
            if(cls==ORDERED_PRIMITIVE_TYPES[i]){
                cost+=0.1f;
                if(i<ORDERED_PRIMITIVE_TYPES.length-1){
                    cls=ORDERED_PRIMITIVE_TYPES[i+1];
                }
            }
        }
        return cost;
    }

    public static Field setAccessible(Field fd){
        if(!Modifier.isPublic(fd.getModifiers())||!Modifier.isPublic(fd.getDeclaringClass().getModifiers())){
            fd.setAccessible(true);
        }
        return fd;
    }

    private Reflects()  {}

}
















