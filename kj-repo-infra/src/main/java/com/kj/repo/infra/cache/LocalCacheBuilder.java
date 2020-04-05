package com.kj.repo.infra.cache;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;

import com.google.common.base.Supplier;
import com.kj.repo.infra.base.builder.Builder;


/**
 * @author kj
 */
public class LocalCacheBuilder<T> implements Builder<LocalCache<T>> {
    private CuratorFramework curator;
    private String notifyPath;
    private Long autoRefreshTime;
    private TimeUnit autoRefreshTimeUnit;
    private Supplier<T> loader;

    public CuratorFramework getCurator() {
        return curator;
    }

    public LocalCacheBuilder<T> setCurator(CuratorFramework curator) {
        this.curator = curator;
        return this;
    }

    public String getNotifyPath() {
        return notifyPath;
    }

    public LocalCacheBuilder<T> setNotifyPath(String notifyPath) {
        this.notifyPath = notifyPath;
        return this;
    }

    public Long getAutoRefreshTime() {
        return autoRefreshTime;
    }

    public LocalCacheBuilder<T> setAutoRefreshTime(Long autoRefreshTime) {
        this.autoRefreshTime = autoRefreshTime;
        return this;
    }

    public TimeUnit getAutoRefreshTimeUnit() {
        return autoRefreshTimeUnit;
    }

    public LocalCacheBuilder<T> setAutoRefreshTimeUnit(TimeUnit autoRefreshTimeUnit) {
        this.autoRefreshTimeUnit = autoRefreshTimeUnit;
        return this;
    }

    public Supplier<T> getLoader() {
        return loader;
    }

    public LocalCacheBuilder<T> setLoader(Supplier<T> loader) {
        this.loader = loader;
        return this;
    }

    @Override
    public LocalCache<T> doBuild() {
        return new LocalCache<>(this);
    }

    @Override
    public void ensure() {

    }

}
