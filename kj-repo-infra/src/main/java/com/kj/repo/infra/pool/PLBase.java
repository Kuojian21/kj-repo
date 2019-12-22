package com.kj.repo.infra.pool;

import org.apache.commons.pool2.impl.GenericObjectPool;

import com.kj.repo.infra.function.Consumer;
import com.kj.repo.infra.function.Function;

/**
 * @author kj
 */
public class PLBase<T> {

    private final GenericObjectPool<T> pool;

    public PLBase(final GenericObjectPool<T> pool) {
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

}
