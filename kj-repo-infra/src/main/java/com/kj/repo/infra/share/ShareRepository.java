package com.kj.repo.infra.share;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author kj
 * Created on 2020-08-29
 */
public class ShareRepository<T> {

    public static final int DEFAULT_LOAD_BATCH_SIZE = 100;
    public static final double DEFAULT_LOAD_BATCH_FACTOR = 0.5D;
    public static final int DEFAULT_CLIENT_SLEEP_MILLS = 0;

    private final ConcurrentMap<Long, ShareCenter<T>> repo = Maps.newConcurrentMap();
    private final Function<Set<Long>, Map<Long, T>> task;
    private final Function<Long, Long> shard;
    private final Supplier<Executor> executor;
    private final int loadBatchSize;
    private final int loadBatchThreshold;
    private final long clientSleepMills;

    public ShareRepository(
            Function<Set<Long>, Map<Long, T>> task, Function<Long, Long> shard,
            Supplier<Executor> executor, int loadBatchSize, double loadBatchFactor, long clientSleepMills) {
        this.task = task;
        this.shard = shard;
        this.executor = executor;
        this.loadBatchSize = loadBatchSize;
        this.loadBatchThreshold = (int) (loadBatchSize * loadBatchFactor);
        this.clientSleepMills = clientSleepMills;
    }

    public static <T> ShareRepository<T> repo(Function<Set<Long>, Map<Long, T>> task, Function<Long, Long> shard,
            Supplier<Executor> executor, int loadBatchSize, double loadBatchFactor, long clientSleepMills) {
        return new ShareRepository<>(task, shard, executor, loadBatchSize, loadBatchFactor, clientSleepMills);
    }

    public static <T> ShareRepository<T> repo(Function<Set<Long>, Map<Long, T>> task, Function<Long, Long> shard,
            Supplier<Executor> executor) {
        return repo(task, shard, executor, DEFAULT_LOAD_BATCH_SIZE, DEFAULT_LOAD_BATCH_FACTOR,
                DEFAULT_CLIENT_SLEEP_MILLS);
    }

    public static <T> ShareRepository<T> repo(Function<Set<Long>, Map<Long, T>> task, Function<Long, Long> shard) {
        return repo(task, shard, MoreExecutors::directExecutor, DEFAULT_LOAD_BATCH_SIZE, DEFAULT_LOAD_BATCH_FACTOR,
                DEFAULT_CLIENT_SLEEP_MILLS);
    }

    public ShareClient<T> client(Collection<Long> ids) {
        return new ShareClient<>(ids, id -> repo
                .computeIfAbsent(shard.apply(id), key -> new ShareCenter<>(task, loadBatchSize, loadBatchThreshold)),
                this.executor, clientSleepMills);
    }
}
