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

public class ShareCenter<T> {
    private final Map<Long, Set<WeakReference<ShareClient<T>>>> idMap = Maps.newHashMap();
    private final Function<Set<Long>, Map<Long, T>> task;
    private final int loadBatchSize;
    private final int loadBatchThreshold;

    public ShareCenter(Function<Set<Long>, Map<Long, T>> task, int loadBatchSize, int loadBatchThreshold) {
        this.task = task;
        this.loadBatchSize = loadBatchSize;
        this.loadBatchThreshold = loadBatchThreshold;
    }

    public void add(Set<Long> ids, WeakReference<ShareClient<T>> reference) {
        synchronized (this) {
            ids.forEach(id -> this.idMap.computeIfAbsent(id, key -> Sets.newHashSet()).add(reference));
        }
    }

    public void run(WeakReference<ShareClient<T>> reference, Set<Long> ids) {
        try {
            if (CollectionUtils.isEmpty(ids)) {
                return;
            }
            Map<Long, Set<ShareClient<T>>> items = Maps.newHashMap();
            synchronized (this) {
                items.putAll(ids.stream()
                        .map(id -> Pair.of(id, Optional.ofNullable(idMap.get(id)).filter(s -> s.contains(reference))
                                .map(s -> idMap.remove(id))
                                .map(s -> s.stream().map(Reference::get).filter(Objects::nonNull)
                                        .collect(Collectors.toSet())).orElse(null)))
                        .filter(pair -> pair.getValue() != null)
                        .collect(Collectors.toMap(Pair::getKey, Pair::getValue)));
                int size = items.size();
                if ((size > 0 || idMap.size() > this.loadBatchThreshold) && size < this.loadBatchSize) {
                    items.putAll(Lists.newArrayList(idMap.keySet()).stream().limit(this.loadBatchSize - size)
                            .map(id -> Pair.of(id, Optional.ofNullable(idMap.remove(id))
                                    .map(s -> s.stream().map(WeakReference::get).filter(
                                            Objects::nonNull).collect(Collectors.toSet())).orElse(null)))
                            .filter(pair -> pair.getValue() != null)
                            .collect(Collectors.toMap(Pair::getKey, Pair::getValue)));
                }
            }
            if (MapUtils.isEmpty(items)) {
                return;
            }
            Map<Long, T> datas = task.apply(items.keySet());
            datas.forEach((k, v) -> items.remove(k).forEach(item -> item.getDataMap().get(k).complete(v)));
            items.forEach((k, v) -> v.forEach(item -> item.getDataMap().get(k).complete(null)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clear(WeakReference<ShareClient<T>> reference, Collection<Long> ids) {
        ids.forEach(id -> Optional.ofNullable(idMap.get(id)).ifPresent(s -> {
            s.remove(reference);
            synchronized (this) {
                Optional.ofNullable(idMap.get(id)).filter(Set::isEmpty)
                        .ifPresent(inner -> idMap.remove(id));
            }
        }));
    }
}
