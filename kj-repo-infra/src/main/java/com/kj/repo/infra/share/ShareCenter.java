package com.kj.repo.infra.share;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author kj
 * Created on 2020-08-27
 */

public class ShareCenter<K, S, V> {
    private final Map<K, Set<WeakReference<ShareClient<K, S, V>>>> keyClients = Maps.newHashMap();
    private final Function<Set<K>, Map<K, V>> task;
    private final int loadBatchSize;
    private final int loadBatchThreshold;

    public ShareCenter(Function<Set<K>, Map<K, V>> task, int loadBatchSize, int loadBatchThreshold) {
        this.task = task;
        this.loadBatchSize = loadBatchSize;
        this.loadBatchThreshold = loadBatchThreshold;
    }

    public void add(Set<K> keys, WeakReference<ShareClient<K, S, V>> reference) {
        synchronized (this) {
            keys.forEach(key -> this.keyClients.computeIfAbsent(key, tKey -> Sets.newHashSet()).add(reference));
        }
    }

    public void run(WeakReference<ShareClient<K, S, V>> reference, Set<K> keys) {
        try {
            if (CollectionUtils.isEmpty(keys)) {
                return;
            }
            Map<K, Set<ShareClient<K, S, V>>> items = Maps.newHashMap();
            synchronized (this) {
                items.putAll(keys.stream()
                        .map(key -> Pair
                                .of(key, Optional.ofNullable(keyClients.get(key)).filter(s -> s.contains(reference))
                                        .map(s -> keyClients.remove(key))
                                        .map(s -> s.stream().map(Reference::get).filter(Objects::nonNull)
                                                .collect(Collectors.toSet())).orElse(null)))
                        .filter(pair -> pair.getValue() != null)
                        .collect(Collectors.toMap(Pair::getKey, Pair::getValue)));
                int size = items.size();
                if ((size > 0 || keyClients.size() > this.loadBatchThreshold) && size < this.loadBatchSize) {
                    items.putAll(Lists.newArrayList(keyClients.keySet()).stream().limit(this.loadBatchSize - size)
                            .map(key -> Pair.of(key, Optional.ofNullable(keyClients.remove(key))
                                    .map(tKeys -> tKeys.stream().map(WeakReference::get).filter(
                                            Objects::nonNull).collect(Collectors.toSet())).orElse(null)))
                            .filter(pair -> pair.getValue() != null)
                            .collect(Collectors.toMap(Pair::getKey, Pair::getValue)));
                }
            }
            if (MapUtils.isEmpty(items)) {
                return;
            }
            Map<K, V> datas = task.apply(items.keySet());
            datas.forEach((key, value) -> items.remove(key).forEach(item -> item.getData().get(key).complete(value)));
            items.forEach((key, value) -> value.forEach(item -> item.getData().get(key).complete(null)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clear(WeakReference<ShareClient<K, S, V>> reference, Collection<K> keys) {
        keys.forEach(key -> Optional.ofNullable(keyClients.get(key)).ifPresent(tKeys -> {
            tKeys.remove(reference);
            synchronized (this) {
                Optional.ofNullable(keyClients.get(key)).filter(Set::isEmpty)
                        .ifPresent(itKeys -> keyClients.remove(key));
            }
        }));
    }
}
