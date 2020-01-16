package com.kj.repo.infra.cache;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;

import org.apache.curator.framework.CuratorFramework;

import com.google.common.base.Supplier;
import com.kj.repo.infra.base.LocalSupplier;
import com.kj.repo.infra.curator.CuratorConf;


/**
 * @author kj
 */
public class LocalCache<T> implements Supplier<T> {

    private final LocalSupplier<T> delegate;

    private final CuratorConf<Long> conf;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    public LocalCache(LocalCacheBuilder<T> builder) {
        this.delegate = new LocalSupplier<T>(builder.getLoader());
        this.conf = new CuratorConf<Long>() {
            @Override
            public CuratorFramework curator() {
                return builder.getCurator();
            }

            @Override
            public String path() {
                return builder.getNotifyPath();
            }

            @Override
            public Long defaultValue() {
                return 0L;
            }

            @Override
            public Long decode(byte[] data) {
                if (data == null && data.length == 0) {
                    return 0L;
                }
                return Long.parseLong(new String(data));
            }

            @Override
            public byte[] encode(Long data) {
                return data.toString().getBytes();
            }

            public BiConsumer<Long, Long> trigger() {
                return (oldValue, newValue) -> delegate.refresh();
            }
        };
        scheduledExecutorService.schedule(this::set, builder.getAutoRefreshTime(), builder.getAutoRefreshTimeUnit());
    }

    @Override
    public T get() {
        return this.delegate.get();
    }

    public void set() {
        this.conf.set(System.currentTimeMillis());
    }

}
