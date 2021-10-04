package com.kj.repo.infra.batch;

import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author kj
 */
public interface BatchBuffer<E, T> {
    static <E, T> BatchBuffer<E, T> queue(int capacity, Function<E, T> mapper) {
        return new QueueBatchBuffer<>(capacity, mapper);
    }

    static <E, K, V> BatchBuffer<E, Entry<K, V>> map(Function<E, K> keyMapper, Function<E, V> valMapper,
            BiFunction<V, V, V> valMerger) {
        return new ConcurrentMapBatchBuffer<>(keyMapper, valMapper, valMerger);
    }

    void add(E element) throws InterruptedException;

    List<T> drainTo(int batchsize);

    int size();

    default boolean isEmpty() {
        return size() <= 0;
    }
}
