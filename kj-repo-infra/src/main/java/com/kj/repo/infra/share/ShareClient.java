package com.kj.repo.infra.share;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;

/**
 * @author kj
 * Created on 2020-08-27
 */
public class ShareClient<K, S, V> {
    private final Supplier<Executor> executor;
    private final Map<K, CompletableFuture<V>> data = Maps.newHashMap();
    private final Function<K, ShareCenter<K, S, V>> shard;
    private final WeakReference<ShareClient<K, S, V>> reference;
    private final long sleepNano;
    private volatile long time;
    private volatile boolean get;

    public ShareClient(Collection<K> keys, Function<K, ShareCenter<K, S, V>> shard, Supplier<Executor> executor,
            long sleepNano) {
        this.shard = shard;
        this.executor = executor;
        this.reference = new WeakReference<>(this);
        this.sleepNano = sleepNano;
        this.time = System.nanoTime();
        this.get = false;
        keys.forEach(key -> data.putIfAbsent(key, new CompletableFuture<>()));
        keys.stream().collect(Collectors.groupingBy(this.shard, Collectors.toSet()))
                .forEach((center, vKeys) -> center.add(vKeys, this.reference));
    }

    Map<K, CompletableFuture<V>> getData() {
        return data;
    }

    public Map<K, V> get() {
        long internal = System.nanoTime() - time;
        if (internal < sleepNano) {
            Uninterruptibles.sleepUninterruptibly(sleepNano - internal, TimeUnit.NANOSECONDS);
        }
        Iterator<Entry<ShareCenter<K, S, V>, Set<K>>> iterator =
                this.data.entrySet().stream().filter(entry -> !entry.getValue().isDone()).map(Map.Entry::getKey)
                        .collect(Collectors.groupingBy(shard, Collectors.toSet())).entrySet().iterator();
        if (iterator.hasNext()) {
            Map.Entry<ShareCenter<K, S, V>, Set<K>> entry = iterator.next();
            while (iterator.hasNext()) {
                Map.Entry<ShareCenter<K, S, V>, Set<K>> tEntry = iterator.next();
                executor.get().execute(() -> tEntry.getKey().run(this.reference));
            }
            entry.getKey().run(this.reference);
        }
        this.get = true;
        return this.data.entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), getUnchecked(entry.getValue())))
                .filter(entry -> entry.getValue() != null).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    @Override
    protected void finalize() {
        if (this.get) {
            return;
        }
        this.data.entrySet().stream().filter(entry -> !entry.getValue().isDone()).map(Map.Entry::getKey)
                .collect(Collectors.groupingBy(shard)).forEach((center, keys) -> center.clear(this.reference));
    }

    public V getUnchecked(Future<V> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}