package com.kj.repo.demo.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * @author kj
 */
public class GuavaCacheHelper<K, V> {

    public Cache<K, V> cache() {
        return CacheBuilder.newBuilder().build();
    }

    public LoadingCache<K, V> cache(CacheLoader<K, V> loader) {
        return CacheBuilder.newBuilder().build(loader);
    }

}
