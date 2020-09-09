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

    public static final int DEFAULT_BATCHSIZE = 100;
    public static final double DEFAULT_FACTOR = 0.5D;

    private final ConcurrentMap<Long, ShareCenter<T>> repo = Maps.newConcurrentMap();
    private final Function<Set<Long>, Map<Long, T>> task;
    private final Function<Long, Long> shard;
    private final Supplier<Executor> executor;
    private final int batchsize;
    private final int threshold;

    public ShareRepository(
            Function<Set<Long>, Map<Long, T>> task, Function<Long, Long> shard,
            Supplier<Executor> executor, int batchsize, double factor) {
        this.task = task;
        this.shard = shard;
        this.executor = executor;
        this.batchsize = batchsize;
        this.threshold = (int) (batchsize * factor);
    }

    public static <T> ShareRepository<T> repo(Function<Set<Long>, Map<Long, T>> task, Function<Long, Long> shard,
            Supplier<Executor> executor, int batchsize, double factor) {
        return new ShareRepository<>(task, shard, executor, batchsize, factor);
    }

    public static <T> ShareRepository<T> repo(Function<Set<Long>, Map<Long, T>> task, Function<Long, Long> shard) {
        return repo(task, shard, MoreExecutors::directExecutor, DEFAULT_BATCHSIZE, DEFAULT_FACTOR);
    }

    public ShareClient<T> client(Collection<Long> ids) {
        return new ShareClient<>(ids, id -> repo
                .computeIfAbsent(shard.apply(id), key -> new ShareCenter<>(task, batchsize, threshold)), this.executor);
    }
}
