package com.kj.repo.infra.pool.jedis;

import java.io.Closeable;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.kj.repo.infra.helper.EnhancerHelper;
import com.kj.repo.infra.pool.PLBase;

import redis.clients.util.Pool;

/**
 * @param <T>
 * @author kj
 */
public class PLJedis<T> extends PLBase<T> {

    public PLJedis(GenericObjectPool<T> pool) {
        super(pool);
    }

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
            public void destroyObject(final PooledObject<T> obj) throws Exception {
                obj.getObject().close();
            }
        }));
    }

    public static <T extends Closeable> T jedis(Pool<T> pool, Class<T> clazz) {
        PLJedis<T> jedis = jedis(pool);
        return EnhancerHelper.enhancer(clazz, (method, args) -> {
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
