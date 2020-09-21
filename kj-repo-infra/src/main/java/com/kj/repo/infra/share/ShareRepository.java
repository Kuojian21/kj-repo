package com.kj.repo.infra.share;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author kj
 * Created on 2020-08-29
 */
public class ShareRepository<K, S, V> {

    public static final int DEFAULT_LOAD_BATCH_SIZE = 100;
    public static final double DEFAULT_LOAD_BATCH_FACTOR = 1.0D;
    public static final int DEFAULT_CLIENT_SLEEP_NANO = 0;

    private final ConcurrentMap<S, ShareCenter<K, S, V>> repo = Maps.newConcurrentMap();
    private final BiFunction<S, Set<K>, Map<K, V>> task;
    private final Function<K, S> shard;
    private final Supplier<Executor> executor;
    private final int loadBatchSize;
    private final int loadBatchThreshold;
    private final long clientSleepNano;

    public ShareRepository(BiFunction<S, Set<K>, Map<K, V>> task, Function<K, S> shard,
            Supplier<Executor> executor, int loadBatchSize, double loadBatchFactor, long clientSleepNano) {
        this.task = task;
        this.shard = shard;
        this.executor = executor;
        this.loadBatchSize = loadBatchSize;
        this.loadBatchThreshold = (int) (loadBatchSize * loadBatchFactor);
        this.clientSleepNano = clientSleepNano;
    }

    public static <K, S, V> ShareRepository<K, S, V> repo(BiFunction<S, Set<K>, Map<K, V>> task, Function<K, S> shard,
            Supplier<Executor> executor, int loadBatchSize, double loadBatchFactor, long clientSleepMills) {
        return new ShareRepository<>(task, shard, executor, loadBatchSize, loadBatchFactor, clientSleepMills);
    }

    public static <K, S, V> ShareRepository<K, S, V> repo(BiFunction<S, Set<K>, Map<K, V>> task,
            Function<K, S> shard, Supplier<Executor> executor) {
        return repo(task, shard, executor, DEFAULT_LOAD_BATCH_SIZE, DEFAULT_LOAD_BATCH_FACTOR,
                DEFAULT_CLIENT_SLEEP_NANO);
    }

    public static <K, S, V> ShareRepository<K, S, V> repo(BiFunction<S, Set<K>, Map<K, V>> task,
            Function<K, S> shard) {
        return repo(task, shard, MoreExecutors::directExecutor, DEFAULT_LOAD_BATCH_SIZE, DEFAULT_LOAD_BATCH_FACTOR,
                DEFAULT_CLIENT_SLEEP_NANO);
    }

    public ShareClient<K, S, V> client(Collection<K> keys) {
        return new ShareClient<>(keys, key -> repo.computeIfAbsent(shard.apply(key),
                sKey -> new ShareCenter<>(sKey, task, loadBatchSize, loadBatchThreshold)), this.executor,
                clientSleepNano);
    }
}
