package com.kj.repo.infra.batch.buffer;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.curator.shaded.com.google.common.collect.Maps;

/**
 * @author kj
 */
public class ConcurrentMapBuffer<E, K, V> implements Buffer<E, Entry<K, V>> {

    private final ConcurrentMap<K, V> map = Maps.newConcurrentMap();
    private final Function<E, K> keyMapper;
    private final Function<E, V> valMapper;
    private final BiFunction<V, V, V> valMerger;

    public ConcurrentMapBuffer(Function<E, K> keyMapper, Function<E, V> valMapper) {
        this(keyMapper, valMapper, (v1, v2) -> v1);
    }

    public ConcurrentMapBuffer(Function<E, K> keyMapper, Function<E, V> valMapper, BiFunction<V, V, V> valMerger) {
        this.keyMapper = keyMapper;
        this.valMapper = valMapper;
        this.valMerger = valMerger;
    }

    @Override
    public void add(E element) throws InterruptedException {
        this.map.merge(keyMapper.apply(element), valMapper.apply(element), valMerger);
    }

    @Override
    public List<Entry<K, V>> drainTo(int batchsize) {
        return map.entrySet().stream().limit(batchsize).collect(Collectors.toList()).stream()
                .filter(e -> map.remove(e.getKey()) != null).collect(Collectors.toList());

    }

    @Override
    public int size() {
        return map.size();
    }

}
