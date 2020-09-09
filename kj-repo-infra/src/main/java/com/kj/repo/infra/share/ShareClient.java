package com.kj.repo.infra.share;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Maps;

/**
 * @author kj
 * Created on 2020-08-27
 */
public class ShareClient<T> {
    private final Supplier<Executor> executor;
    private final Map<Long, CompletableFuture<T>> dataMap = Maps.newHashMap();
    private final Function<Long, ShareCenter<T>> shard;
    private final WeakReference<ShareClient<T>> reference;
    private volatile boolean get = false;

    public ShareClient(Collection<Long> ids, Function<Long, ShareCenter<T>> shard, Supplier<Executor> executor) {
        this.shard = shard;
        this.executor = executor;
        this.reference = new WeakReference<>(this);
        this.add(ids);
    }

    Map<Long, CompletableFuture<T>> getDataMap() {
        return dataMap;
    }

    public void add(Collection<Long> ids) {
        this.get = false;
        ids.forEach(id -> dataMap.putIfAbsent(id, new CompletableFuture<>()));
        ids.stream().collect(Collectors.groupingBy(this.shard, Collectors.toSet()))
                .forEach((k, v) -> k.add(v, this.reference));
    }

    public Map<Long, T> get() {
        this.dataMap.entrySet().stream()
                .collect(Collectors.groupingBy(e -> shard.apply(e.getKey()), Collectors.toSet()))
                .forEach((center, vSet) -> executor.get().execute(() -> center
                        .run(this.reference, vSet.stream().filter(e -> !e.getValue().isDone()).map(Map.Entry::getKey)
                                .collect(Collectors.toSet()))));
        this.get = true;
        return this.dataMap.entrySet().stream()
                .map(e -> Pair.of(e.getKey(), getUnchecked(e.getValue())))
                .filter(e -> e.getValue() != null).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    @Override
    protected void finalize() {
        if (this.get) {
            return;
        }
        this.dataMap.entrySet().stream().filter(e -> !e.getValue().isDone()).map(Map.Entry::getKey)
                .collect(Collectors.groupingBy(shard)).forEach((center, v) -> center.clear(this.reference, v));
    }

    public T getUnchecked(Future<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
