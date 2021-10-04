package com.kj.repo.demo.cache;

import com.google.common.base.Supplier;
import org.apache.curator.framework.CuratorFramework;

import java.util.concurrent.TimeUnit;


/**
 * @author kj
 */
public class LocalCacheBuilder<T> {
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

    public LocalCache<T> build() {
        return new LocalCache<>(this);
    }

    public void ensure() {

    }

}
