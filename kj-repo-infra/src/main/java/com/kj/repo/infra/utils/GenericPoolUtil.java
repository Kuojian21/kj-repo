package com.kj.repo.infra.utils;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.kj.repo.infra.Consumer;
import com.kj.repo.infra.Supplier;

/**
 * @author kj
 * Created on 2020-03-14
 */
public class GenericPoolUtil {

    public static <T> GenericObjectPool<T> wrap(Supplier<T> create, Consumer<T> destroy) {
        return new GenericObjectPool<>(new BasePooledObjectFactory<T>() {
            @Override
            public T create() throws Exception {
                return create.get();
            }

            @Override
            public PooledObject<T> wrap(T obj) {
                return new DefaultPooledObject<>(obj);
            }

            @Override
            public void destroyObject(final PooledObject<T> obj) throws Exception {
                destroy.accept(obj.getObject());
            }
        });
    }
}
