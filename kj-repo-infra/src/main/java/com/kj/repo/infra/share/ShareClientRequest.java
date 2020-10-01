package com.kj.repo.infra.share;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author kj
 * Created on 2020-09-21
 */
public class ShareClientRequest<K, V> {
    private final Set<K> keys;
    private final CompletableFuture<Map<K, V>> value;

    public ShareClientRequest(Set<K> keys) {
        this.keys = keys;
        this.value = new CompletableFuture<>();
    }

    public void setThrowable(Throwable throwable) {
        this.value.obtrudeException(throwable);
    }

    public Map<K, V> getValue() {
        try {
            return this.value.get();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void setValue(Map<K, V> valueMap) {
        this.value.complete(this.keys.stream().filter(valueMap::containsKey)
                .collect(Collectors.toMap(Function.identity(), valueMap::get)));
    }
}
