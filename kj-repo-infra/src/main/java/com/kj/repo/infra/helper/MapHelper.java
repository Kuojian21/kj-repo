package com.kj.repo.infra.helper;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * @author kuojian21
 */
public class MapHelper {
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> newHashMap(Object... objs) {
        Map<K, V> result = Maps.newHashMap();
        for (int i = 0, len = objs.length; i < len; i += 2) {
            result.put((K) objs[i], (V) objs[i + 1]);
        }
        return result;
    }
}
