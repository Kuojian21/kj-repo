package com.kj.repo.infra.share;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author kj
 * Created on 2020-08-27
 */

public class ShareCenter<K, S, V> {
    private final ConcurrentMap<ShareClientRequest<K, S, V>, Set<K>> iRequests = new ConcurrentHashMap<>(512);
    private final BiFunction<S, Set<K>, Map<K, List<V>>> task;
    private final S sKey;
    private final int loadBatchSize;
    private final int loadBatchThreshold;
    private final Lock loadLock;
    private final ShareClientRequest<K, S, V> empty =
            new ShareClientRequest<>(Sets.newHashSet(), new WeakReference<>(null));


    public ShareCenter(S sKey, BiFunction<S, Set<K>, Map<K, List<V>>> task, int loadBatchSize, int loadBatchThreshold,
            Lock loadLock) {
        this.sKey = sKey;
        this.task = task;
        this.loadBatchSize = loadBatchSize;
        this.loadBatchThreshold = loadBatchThreshold;
        this.loadLock = loadLock;
    }

    public ShareClientRequest<K, S, V> add(Set<K> keys, WeakReference<ShareClient<K, S, V>> reference) {
        ShareClientRequest<K, S, V> request = new ShareClientRequest<>(keys, reference);
        iRequests.put(request, keys);
        return request;
    }

    public void run(List<ShareClientRequest<K, S, V>> requests) {
        Map<ShareClientRequest<K, S, V>, Set<K>> cRequestMap =
                this.loadLock == null ? requests(requests) : this.runInLock(this.loadLock, () -> requests(requests));
        if (MapUtils.isEmpty(cRequestMap)) {
            return;
        }
        try {
            Map<K, List<V>> datas =
                    task.apply(this.sKey, cRequestMap.entrySet().stream().flatMap(e -> e.getValue().stream())
                            .collect(Collectors.toSet()));
            cRequestMap.keySet().forEach(request -> request.setValue(datas));
        } catch (Throwable throwable) {
            cRequestMap.keySet().forEach(request -> request.setThrowable(throwable));
        }
    }

    public void clear(List<ShareClientRequest<K, S, V>> requests) {
        requests.forEach(iRequests::remove);
    }

    private Map<ShareClientRequest<K, S, V>, Set<K>> requests(List<ShareClientRequest<K, S, V>> requests) {
        Map<ShareClientRequest<K, S, V>, Set<K>> cRequestMap = new HashMap<>();
        cRequestMap.put(empty, Sets.newHashSet());
        cRequestMap.remove(empty);

        List<ShareClientRequest<K, S, V>> rRequests = Lists.newArrayList(this.iRequests.keySet());
        int rRequestSize = rRequests.size();

        int cRequestSize = 0;
        for (int i = 0, len = requests.size(); i < len; i++) {
            ShareClientRequest<K, S, V> request = requests.get(i);
            Set<K> tKeys = this.iRequests.remove(request);
            if (tKeys != null) {
                cRequestMap.put(request, tKeys);
                cRequestSize += tKeys.size();
            }
        }
        if ((cRequestSize <= 0 || cRequestSize >= this.loadBatchSize) && rRequestSize < this.loadBatchThreshold) {
            return cRequestMap;
        }
        for (ShareClientRequest<K, S, V> request : rRequests) {
            Set<K> tKeys = this.iRequests.remove(request);
            if (tKeys != null) {
                cRequestMap.put(request, tKeys);
                cRequestSize += tKeys.size();
                if (cRequestSize >= this.loadBatchSize) {
                    return cRequestMap;
                }
            }
        }
        return cRequestMap;
    }

    private Map<ShareClientRequest<K, S, V>, Set<K>> runInLock(Lock lock,
            Supplier<Map<ShareClientRequest<K, S, V>, Set<K>>> supplier) {
        try {
            lock.lock();
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }
}
