package com.kj.repo.infra.share;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.phantomthief.util.MoreSuppliers;
import com.google.common.collect.Maps;

/**
 * @author kj
 * Created on 2020-08-29
 */
public class ShareRepository<T> {
    private static final Supplier<ExecutorService> EXECUTOR =
            MoreSuppliers.lazy(() -> Executors.newFixedThreadPool(100, r -> {
                Thread thread = new Thread(r);
                thread.setName("share-pool");
                thread.setDaemon(true);
                return thread;
            }));
    private final ConcurrentMap<Long, ShareCenter<T>> repo = Maps.newConcurrentMap();
    private final Function<Set<Long>, Map<Long, T>> task;
    private final Function<Long, Long> shard;

    public ShareRepository(Function<Set<Long>, Map<Long, T>> task, Function<Long, Long> shard) {
        this.task = task;
        this.shard = shard;
    }

    public static <T> ShareRepository<T> repo(Function<Set<Long>, Map<Long, T>> task, Function<Long, Long> shard) {
        return new ShareRepository<>(task, shard);
    }

    public ShareClient<T> client(Collection<Long> ids) {
        return new ShareClient<>(ids, id -> repo.computeIfAbsent(shard.apply(id), key -> new ShareCenter<>(task)),
                EXECUTOR);
    }
}
