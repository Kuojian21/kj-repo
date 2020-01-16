package com.kj.repo.infra.curator;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

/**
 * @author kj
 */
public class CuratorHelper {

    private static final ConcurrentMap<CuratorConf<?>, Curator<?>> MAP = Maps.newConcurrentMap();

    public static <T> Curator<T> curator(CuratorConf<T> conf) {
        Curator<?> curator = MAP.get(conf);
        if (curator == null) {
            synchronized (CuratorHelper.class) {
                curator = MAP.get(conf);
                if (curator == null) {
                    curator = new Curator<>(conf);
                    MAP.put(conf, curator);
                }
            }
        }
        return (Curator<T>) curator;
    }

}
