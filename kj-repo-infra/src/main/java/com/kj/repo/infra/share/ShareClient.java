package com.kj.repo.infra.share;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
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
    private final long sleepNano;
    private final WeakReference<ShareClient<K, S, V>> reference = new WeakReference<>(this);
    private final Map<ShareCenter<K, S, V>, List<ShareClientRequest<K, S, V>>> requests = Maps.newHashMap();
    private volatile long time;
    private volatile boolean get = true;

    public ShareClient(Collection<K> keys, Function<K, ShareCenter<K, S, V>> shard, Supplier<Executor> executor,
            long sleepNano) {
        this.shard = shard;
        this.executor = executor;
        this.sleepNano = sleepNano;
        this.add(keys);
    }

    public void add(Collection<K> keys) {
        if (this.get) {
            this.time = System.nanoTime();
            this.get = false;
        }
        keys.forEach(key -> data.putIfAbsent(key, new CompletableFuture<>()));
        keys.stream().collect(Collectors.groupingBy(this.shard, Collectors.toSet()))
                .forEach((center, vKeys) -> this.requests.computeIfAbsent(center, t -> Lists.newArrayList())
                        .add(center.add(vKeys, this.reference)));
    }

    Map<K, CompletableFuture<V>> getData() {
        return data;
    }

    public Map<K, V> get() {
        this.get = true;
        long internal = System.nanoTime() - time;
        if (internal < sleepNano) {
            Uninterruptibles.sleepUninterruptibly(sleepNano - internal, TimeUnit.NANOSECONDS);
        }
        List<ShareCenter<K, S, V>> centers =
                new ArrayList<>(
                        this.data.entrySet().stream().filter(entry -> !entry.getValue().isDone()).map(Entry::getKey)
                                .collect(Collectors.groupingBy(shard, Collectors.toSet())).keySet());
        if (CollectionUtils.isNotEmpty(centers)) {
            for (int i = 1, len = centers.size(); i < len; i++) {
                ShareCenter<K, S, V> center = centers.get(i);
                executor.get().execute(() -> center.run(this.requests.get(center)));
            }
            ShareCenter<K, S, V> center = centers.get(0);
            center.run(this.requests.get(center));
        }
        return this.data.entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), getUnchecked(entry.getValue())))
                .filter(entry -> entry.getValue() != null).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    @Override
    protected void finalize() {
        if (this.get) {
            return;
        }
        this.requests.forEach(ShareCenter::clear);
    }

    public V getUnchecked(Future<V> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}