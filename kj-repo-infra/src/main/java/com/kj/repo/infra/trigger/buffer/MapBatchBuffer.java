package com.kj.repo.infra.trigger.buffer;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.curator.shaded.com.google.common.collect.Maps;

/**
 * @author kj
 */
public class MapBatchBuffer<E, K, V> implements BatchBuffer<E, Map.Entry<K, V>> {

    private final ConcurrentMap<K, V> map = Maps.newConcurrentMap();
    private final Function<E, K> keyMapper;
    private final Function<E, V> valueInit;
    private final BiConsumer<E, V> valueHandle;

    public MapBatchBuffer(Function<E, K> keyMapper, Function<E, V> valueInit) {
        this(keyMapper, valueInit, (e, v) -> {
            return;
        });
    }

    public MapBatchBuffer(Function<E, K> keyMapper, Function<E, V> valueInit, BiConsumer<E, V> valueHandle) {
        super();
        this.keyMapper = keyMapper;
        this.valueInit = valueInit;
        this.valueHandle = valueHandle;
    }

    @Override
    public void add(E element) throws InterruptedException {
        V value = this.map.computeIfAbsent(keyMapper.apply(element), k -> valueInit.apply(element));
        this.valueHandle.accept(element, value);
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
