package com.kj.repo.infra.utils;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * @author kj
 */
public class MapUtil {
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> newHashMap(Object... objs) {
        Map<K, V> result = Maps.newHashMap();
        for (int i = 0, len = objs.length; i < len; i += 2) {
            result.put((K) objs[i], (V) objs[i + 1]);
        }
        return result;
    }

    public static <K, V> ImmutableMap<K, V> of(K k1, V v1) {
        return ImmutableMap.of(k1, v1);
    }
}
