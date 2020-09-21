package com.kj.repo.infra.share;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
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
        Map<WeakReference<ShareClient<K, S, V>>, Set<K>> clientRefMap = new HashMap<>();
        clientRefMap.put(reference, Sets.newHashSet());
        Iterator<WeakReference<ShareClient<K, S, V>>> iterator =
                Lists.newArrayList(this.clients.keySet()).iterator();
        Set<K> keys = clients.remove(reference);
        if (keys == null) {
            return;
        }
        while (iterator.hasNext()) {
            WeakReference<ShareClient<K, S, V>> clientRef = iterator.next();
            Set<K> tKeys = clients.remove(clientRef);
            if (tKeys != null) {
                clientRefMap.put(clientRef, tKeys);
            }
        }
        try {
            clientRefMap.put(reference, keys);
            Map<K, List<ShareClient<K, S, V>>> clientMap =
                    clientRefMap.entrySet().stream().map(e -> Pair.of(e.getKey().get(), e.getValue()))
                            .filter(pair -> pair.getKey() != null && pair.getValue() != null)
                            .flatMap(pair -> pair.getValue().stream().map(key -> Pair.of(key, pair.getKey())))
                            .collect(Collectors.groupingBy(Pair::getKey)).entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey,
                                    e -> e.getValue().stream().map(Pair::getValue).collect(Collectors.toList())));
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
