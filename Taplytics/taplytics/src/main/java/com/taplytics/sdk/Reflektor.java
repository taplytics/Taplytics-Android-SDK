/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;


        import java.lang.reflect.Field;
        import java.lang.reflect.InvocationTargetException;
        import java.lang.reflect.Method;
        import java.util.HashMap;

/**
 * Created by clvcooke on 10/27/2015.
 */
public class Reflektor {

    private Class targetClass;
    private Object target;
    private HashMap<String, Method> methodMap = new HashMap<>();

    /**
     * @param classPath the full path of the target class
     * @throws IllegalAccessException the class can not be publicly accessed
     * @throws InstantiationException given class does not have a constructor
     * @throws ClassNotFoundException the class name given does not resolve to an actual class
     */
    public Reflektor(String classPath) throws Exception, Error{
        targetClass = Class.forName(classPath);
        target = targetClass.newInstance();
    }

    /**
     * @param classPath  the full path of the target class
     * @param parameters an array of the parameters (if using Reflektor objects call #getClient on them before adding them to the array)
     * @throws ClassNotFoundException    the class name given does not resolve to an actual class
     * @throws NoSuchMethodException     there is no constructor with these parameters
     * @throws IllegalAccessException    the class/method can not be publicly accessed
     * @throws InvocationTargetException
     * @throws InstantiationException    the constructor is not valid
     */

    public Reflektor(String classPath,Class[] classes, Object[] parameters) throws Exception, Error {
        targetClass = Class.forName(classPath);
        target = targetClass.getConstructor(classes).newInstance(parameters);
    }

    /**
     * @param classPath  the full path of the target class
     * @param methodName the method used instead of a normal constructor
     * @throws ClassNotFoundException    the class name given does not resolve to an actual class
     * @throws NoSuchMethodException     there is no method with this name
     * @throws IllegalAccessException    the class/method can not be publicly accessed
     * @throws InvocationTargetException
     * @throws InstantiationException    the constructor is not valid
     */

    public Reflektor(String classPath, String methodName) throws Exception, Error {
        targetClass = Class.forName(classPath);
        Method m = targetClass.getDeclaredMethod(methodName);
        target = m.invoke(null);
    }


    /**
     * @param classPath  the full path of the target class
     * @param methodName the method used instead of a normal constructor
     * @param parameters an array of the parameters (if using Reflektor objects call #getClient on them before adding them to the array)\
     * @throws ClassNotFoundException    the class name given does not resolve to an actual class
     * @throws NoSuchMethodException     there is no method with this name
     * @throws IllegalAccessException    the class/method can not be publicly accessed
     * @throws InvocationTargetException
     * @throws InstantiationException    the method is not valid
     */

    public Reflektor(String classPath, String methodName, Class[] classes, Object[] parameters) throws Exception, Error {
        targetClass = Class.forName(classPath);
        Method m = targetClass.getDeclaredMethod(methodName, classes);
        target = m.invoke(null, parameters);
    }


    /**
     * In case you already have the object you want to use, but want to wrap it in reflektor
     * @param object the object that you want to wrap in Reflektor
     *
     */

    public Reflektor(Object object) {
        targetClass = object.getClass();
        target = object;
    }



    /**
     * @param methodName the method which you want to invoke
     * @return the return value of the method (ignore if void)
     * @throws NoSuchMethodException     the method name given was incorrect
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */

    public Object invoke(String methodName) throws Exception, Error {
        if (!methodMap.containsKey(methodName)) {
            methodMap.put(methodName, targetClass.getDeclaredMethod(methodName));
        }
        return methodMap.get(methodName).invoke(target);
    }

    /**
     * @param methodName the method which you want to invoke
     * @param parameters the parameters for the method
     * @return the return value of the method (ignore if void)
     * @throws NoSuchMethodException     the method name given was incorrect or the parameters were incorrect
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */

    public Object invoke(String methodName, Class[] classes, Object[] parameters) throws Exception, Error {
        //for now....do some speed tests later
        int length = classes.length;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(methodName);
        for (int i = 0; i < length; i++) {
            stringBuilder.append(classes[i].toString());
        }
        String fullMethodName = stringBuilder.toString();
        if (!methodMap.containsKey(fullMethodName)) {
            Method m = targetClass.getDeclaredMethod(methodName, classes);
            m.setAccessible(true);
            methodMap.put(fullMethodName, m);
        }
        return methodMap.get(fullMethodName).invoke(target, parameters);
    }

    /**
     * @param fieldName the name of the variable
     * @return the variables value
     * @throws NoSuchFieldException   the field name is incorrect
     * @throws IllegalAccessException
     */

    public Object getVar(String fieldName) throws Exception, Error {
        Field f = targetClass.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * @param fieldName the name of the variable
     * @param value     the variables value
     * @throws NoSuchFieldException   the field name is incorrect
     * @throws IllegalAccessException
     */

    public void setVar(String fieldName, Object value) throws Exception, Error {
        Field f = targetClass.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }


    /**
     * @param classPath  the full path of the class
     * @param methodName the name of the method
     * @return the return value of the method (ignore if void)
     * @throws ClassNotFoundException    the class path given is incorrect
     * @throws NoSuchMethodException     the method name is wrong or the method is public
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */

    public static Object invokeStatic(String classPath, String methodName) throws Exception, Error {
        Method m = Class.forName(classPath).getMethod(methodName);
        m.setAccessible(true);
        return m.invoke(null);
    }

    /**
     * @param classPath  the full path of the class
     * @param methodName the name of the method
     * @param parameters the parameters of the method
     * @return the return value of the method (ignore if void)
     * @throws ClassNotFoundException    the class path given is incorrect
     * @throws NoSuchMethodException     the method name is wrong or the method is public or the parameters are wrong
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */

    public static Object invokeStatic(String classPath, String methodName, Class[] classes, Object[] parameters) throws Exception, Error {
        Method m = Class.forName(classPath).getMethod(methodName, classes);
        m.setAccessible(true);
        return m.invoke(null, parameters);
    }

    /**
     * @param classPath the full path of the class
     * @param fieldName the name of the variable
     * @return the value of the variable
     * @throws ClassNotFoundException the class path given is incorrect
     * @throws NoSuchFieldException   the variable name is incorrect
     * @throws IllegalAccessException
     */

    public static Object getStaticVar(String classPath, String fieldName) throws Exception, Error{
        Field f = Class.forName(classPath).getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(null);
    }

    /**
     * @return the class of the target object
     */

    public Class getTargetClass() {
        return targetClass;
    }

    /**
     * @return the target object
     */
    public Object getTarget() {
        return target;
    }

}
