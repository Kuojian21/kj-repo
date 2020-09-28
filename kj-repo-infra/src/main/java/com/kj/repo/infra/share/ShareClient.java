package com.kj.repo.infra.share;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
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
    private final Function<K, Set<ShareCenter<K, S, V>>> shard;
    private final long sleepNano;
    private final WeakReference<ShareClient<K, S, V>> reference = new WeakReference<>(this);
    private final Map<ShareCenter<K, S, V>, List<ShareClientRequest<K, S, V>>> requests = Maps.newHashMap();
    private volatile long time;
    private volatile boolean get = true;

    public ShareClient(Collection<K> keys, Function<K, Set<ShareCenter<K, S, V>>> shard, Supplier<Executor> executor,
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
        keys.stream().flatMap(key -> this.shard.apply(key).stream().map(center -> Pair.of(center, key)))
                .collect(Collectors.groupingBy(Pair::getKey))
                .forEach((center, vKeys) -> this.requests.computeIfAbsent(center, t -> Lists.newArrayList())
                        .add(center
                                .add(vKeys.stream().map(Pair::getValue).collect(Collectors.toSet()), this.reference)));
    }

    public Map<K, List<V>> get() {
        this.get = true;
        long internal = System.nanoTime() - time;
        if (internal < sleepNano) {
            Uninterruptibles.sleepUninterruptibly(sleepNano - internal, TimeUnit.NANOSECONDS);
        }
        List<Map.Entry<ShareCenter<K, S, V>, List<ShareClientRequest<K, S, V>>>> tRequests =
                Lists.newArrayList(this.requests.entrySet());
        if (CollectionUtils.isNotEmpty(tRequests)) {
            for (int i = 1, len = tRequests.size(); i < len; i++) {
                Map.Entry<ShareCenter<K, S, V>, List<ShareClientRequest<K, S, V>>> request = tRequests.get(i);
                executor.get().execute(() -> request.getKey().run(request.getValue()));
            }
            Map.Entry<ShareCenter<K, S, V>, List<ShareClientRequest<K, S, V>>> request = tRequests.get(0);
            executor.get().execute(() -> request.getKey().run(request.getValue()));
        }
        return this.requests.values().stream().flatMap(requests -> requests.stream()
                .flatMap(request -> request.getValue().entrySet().stream()))
                .collect(Collectors.groupingBy(Map.Entry::getKey))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().flatMap(t -> t.getValue().stream())
                                .collect(Collectors.toList())));
    }

    @Override
    protected void finalize() {
        if (this.get) {
            return;
        }
        this.requests.forEach(ShareCenter::clear);
    }
}