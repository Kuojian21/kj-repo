package com.kj.repo.infra.pool.crypt;

import org.apache.commons.pool2.impl.GenericObjectPool;

import com.kj.repo.infra.pool.PLBase;

/**
 * @param <T>
 * @author kj
 */
public abstract class PLCrypt<T> extends PLBase<T> {

    public PLCrypt(final GenericObjectPool<T> pool) {
        super(pool);
    }

}
