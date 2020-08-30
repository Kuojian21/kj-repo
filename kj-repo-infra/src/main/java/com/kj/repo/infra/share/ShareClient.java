package com.kj.repo.infra.share;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
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
    private final Supplier<ExecutorService> executor;
    private final Map<Long, CompletableFuture<T>> dataMap = Maps.newHashMap();
    private final Function<Long, ShareCenter<T>> shard;
    private final WeakReference<ShareClient<T>> reference;
    private volatile boolean get = false;

    public ShareClient(Collection<Long> ids, Function<Long, ShareCenter<T>> shard, Supplier<ExecutorService> executor) {
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
        Iterator<Map.Entry<ShareCenter<T>, Set<Long>>> iterator =
                this.dataMap.entrySet().stream().filter(e -> !e.getValue().isDone()).map(Map.Entry::getKey)
                        .collect(Collectors.groupingBy(shard, Collectors.toSet())).entrySet().iterator();
        if (iterator.hasNext()) {
            Map.Entry<ShareCenter<T>, Set<Long>> shareCenter = iterator.next();
            while (iterator.hasNext()) {
                Map.Entry<ShareCenter<T>, Set<Long>> tShareCenter = iterator.next();
                executor.get().submit(() -> tShareCenter.getKey().run(this, tShareCenter.getValue()));
            }
            shareCenter.getKey().run(this, shareCenter.getValue());
        }
        this.get = true;
        return this.dataMap.entrySet().stream()
                .map(e -> Pair.of(e.getKey(), getUnchecked(e.getValue())))
                .filter(e -> e.getValue() != null).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    @Override
    protected void finalize() throws Throwable {
        if (this.get) {
            return;
        }
        this.dataMap.entrySet().stream().filter(e -> !e.getValue().isDone()).map(Map.Entry::getKey)
                .collect(Collectors.groupingBy(shard)).forEach((k, v) -> {
            k.clear(this.reference, v);
        });
    }

    public T getUnchecked(Future<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
