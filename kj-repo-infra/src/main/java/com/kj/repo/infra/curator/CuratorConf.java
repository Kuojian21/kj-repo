package com.kj.repo.infra.curator;

import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

import org.apache.curator.framework.CuratorFramework;

import com.google.common.util.concurrent.MoreExecutors;
import com.kj.repo.infra.base.Conf;

/**
 * @author kj
 */
public interface CuratorConf<T> extends Conf<T> {

    CuratorFramework curator();

    default T get() {
        return CuratorHelper.curator(this).get();
    }

    default void set(T data) {
        CuratorHelper.curator(this).set(data);
    }

    default BiConsumer<T, T> trigger() {
        return (oldValue, newValue) -> {

        };
    }

    default Executor executor() {
        return MoreExecutors.directExecutor();
    }

}
