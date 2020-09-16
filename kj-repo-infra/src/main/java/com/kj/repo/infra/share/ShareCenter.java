package com.kj.repo.infra.share;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author kj
 * Created on 2020-08-27
 */

public class ShareCenter<K, S, V> {
    private final ConcurrentMap<WeakReference<ShareClient<K, S, V>>, Set<K>> clients = new ConcurrentHashMap<>(512);
    private final BiFunction<S, Set<K>, Map<K, V>> task;
    private final S sKey;
    private final int loadBatchSize;
    private final int loadBatchThreshold;

    public ShareCenter(S sKey, BiFunction<S, Set<K>, Map<K, V>> task, int loadBatchSize, int loadBatchThreshold) {
        this.sKey = sKey;
        this.task = task;
        this.loadBatchSize = loadBatchSize;
        this.loadBatchThreshold = loadBatchThreshold;
    }

    public void add(Set<K> keys, WeakReference<ShareClient<K, S, V>> reference) {
        clients.put(reference, keys);
    }

    public void run(WeakReference<ShareClient<K, S, V>> reference) {
        try {
            Map<K, Set<ShareClient<K, S, V>>> clientMap = Maps.newHashMap();
            Set<K> keys = clients.remove(reference);
            if (CollectionUtils.isNotEmpty(keys)) {
                keys.forEach(key -> clientMap.computeIfAbsent(key, tKey -> Sets.newHashSet()).add(reference.get()));
            } else if (clients.size() < loadBatchThreshold) {
                return;
            }
            Iterator<WeakReference<ShareClient<K, S, V>>> iterator =
                    Lists.newArrayList(this.clients.keySet()).iterator();
            while (clientMap.size() < this.loadBatchSize && iterator.hasNext()) {
                Optional.ofNullable(iterator.next())
                        .map(clientRef -> Pair.of(clientRef.get(), clients.remove(clientRef)))
                        .filter(pair -> pair.getKey() != null && pair.getValue() != null)
                        .ifPresent(pair -> pair.getValue().forEach(
                                key -> clientMap.computeIfAbsent(key, tKey -> Sets.newHashSet()).add(pair.getKey())));
            }
            Map<K, V> datas = task.apply(this.sKey, clientMap.keySet());
            datas.forEach((k, v) -> clientMap.remove(k).forEach(client -> client.getData().get(k).complete(v)));
            clientMap.forEach((k, v) -> v.forEach(client -> client.getData().get(k).complete(null)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clear(WeakReference<ShareClient<K, S, V>> reference) {
        clients.remove(reference);
    }
}
