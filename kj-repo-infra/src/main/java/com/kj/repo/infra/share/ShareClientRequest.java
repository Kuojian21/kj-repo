package com.kj.repo.infra.share;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author kj
 * Created on 2020-09-21
 */
public class ShareClientRequest<K, S, V> {
    private final Set<K> keys;
    private final WeakReference<ShareClient<K, S, V>> client;
    private final CompletableFuture<Map<K, List<V>>> value;

    public ShareClientRequest(Set<K> keys, WeakReference<ShareClient<K, S, V>> client) {
        this.keys = keys;
        this.client = client;
        this.value = new CompletableFuture<>();
    }

    public void setValue(Map<K, List<V>> valueMap) {
        this.value.complete(this.keys.stream().filter(valueMap::containsKey)
                .collect(Collectors.toMap(Function.identity(), valueMap::get)));
    }

    public void setThrowable(Throwable throwable) {
        this.value.obtrudeException(throwable);
    }

    public Map<K, List<V>> getValue() {
        try {
            return this.value.get();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
