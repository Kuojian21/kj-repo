package com.kj.repo.infra.helper;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * @author kj
 */
@SuppressWarnings("unchecked")
public class EnhancerHelper {

    public static <T> T enhancer(Class<T> clazz, BiFunction<Method, Object[], Object> func) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                return func.apply(method, args);
            }
        });
        return (T) enhancer.create();
    }
}