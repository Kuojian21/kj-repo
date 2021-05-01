package com.kj.repo.infra.cache;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import com.kj.repo.infra.base.LocalSupplier;


/**
 * @author kj
 */
public class LocalCache<T> implements Supplier<T> {

    private final LocalSupplier<T> delegate;
//    private final Curator<Long> curator;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    public LocalCache(LocalCacheBuilder<T> builder) {
        this.delegate = new LocalSupplier<>(builder.getLoader());
//        this.curator = new Curator<>("notify", new Conf<Long>() {
//            @Override
//            public String name() {
//                return builder.getNotifyPath();
//            }
//
//            @Override
//            public Long get() {
//                return 0L;
//            }
//        }, builder.getCurator(), bytes -> {
//            this.delegate.refresh();
//            return Long.parseLong(new String(bytes));
//        });
        scheduledExecutorService
                .schedule(this::refresh, builder.getAutoRefreshTime(), builder.getAutoRefreshTimeUnit());
    }

    @Override
    public T get() {
        return this.delegate.get();
    }

    public void refresh() {
//        this.curator.set((System.currentTimeMillis() + "").getBytes());
    }

}
