package com.kj.repo.infra.pool;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.function.BiFunction;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import redis.clients.util.Pool;

/**
 * @param <T>
 * @author kuojian21
 */
public class PLJedis<T> {

    private final GenericObjectPool<T> pool;

    public PLJedis(final GenericObjectPool<T> pool) {
        this.pool = pool;
    }

    public final <R> R execute(Function<T, R> function) throws Exception {
        T t = null;
        try {
            t = this.pool.borrowObject();
            return function.apply(t);
        } finally {
            if (t != null) {
                this.pool.returnObject(t);
            }
        }
    }

    public final void execute(Consumer<T> consumer) throws Exception {
        T t = null;
        try {
            t = this.pool.borrowObject();
            consumer.accept(t);
        } finally {
            if (t != null) {
                this.pool.returnObject(t);
            }
        }
    }

    /**
     * @author kuojian21
     */
    @FunctionalInterface
    public static interface Function<T, R> {
        R apply(T t) throws Exception;
    }

    /**
     * @author kuojian21
     */
    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T t) throws Exception;
    }

    /**
     * @author kuojian21
     */
    public static class Jedis {
        public static <T extends Closeable> PLJedis<T> jedis(Pool<T> pool) {
            return new PLJedis<T>(new GenericObjectPool<T>(new BasePooledObjectFactory<T>() {
                @Override
                public T create() throws Exception {
                    return pool.getResource();
                }

                @Override
                public PooledObject<T> wrap(T obj) {
                    return new DefaultPooledObject<T>(obj);
                }

                @Override
                public void destroyObject(final PooledObject<T> obj)
                        throws Exception {
                    obj.getObject().close();
                }
            }) {
                @Override
                public void close() {
                    pool.close();
                    super.close();
                }
            });
        }

        public static <T extends Closeable> T jedis(Pool<T> pool, Class<T> clazz) {
            PLJedis<T> jedis = jedis(pool);
            return Helper.enhancer(clazz, (method, args) -> {
                try {
                    return jedis.execute(f -> {
                        return method.invoke(f, args);
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    /**
     * @author kuojian21
     */
    public static class Helper {

        @SuppressWarnings("unchecked")
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

}
