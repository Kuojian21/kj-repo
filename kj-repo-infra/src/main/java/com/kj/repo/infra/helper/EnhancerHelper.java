package com.kj.repo.infra.helper;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

/**
 * @author kj
 */
@SuppressWarnings("unchecked")
public class EnhancerHelper {

    public static <T> T enhancer(Class<T> clazz, BiFunction<Method, Object[], Object> func) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> func.apply(method, args));
        return (T) enhancer.create();
    }
}