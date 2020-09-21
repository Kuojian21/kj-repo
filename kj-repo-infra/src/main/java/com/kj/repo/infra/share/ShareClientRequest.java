package com.kj.repo.infra.share;

import java.lang.ref.WeakReference;

/**
 * @author kj
 * Created on 2020-09-21
 */
public class ShareClientRequest<K, S, V> {
    private final WeakReference<ShareClient<K, S, V>> client;

    public ShareClientRequest(WeakReference<ShareClient<K, S, V>> client) {
        this.client = client;
    }

    public ShareClient<K, S, V> getClient() {
        return client.get();
    }
}
