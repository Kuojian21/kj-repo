package com.kj.repo.infra.trigger;

import java.util.List;

/**
 * @author kj
 */
public interface BatchBuffer<E, T> {

    void add(E element) throws InterruptedException;

    List<T> drainTo(int batchsize);

    int size();

    default boolean isEmpty() {
        return size() <= 0;
    }
}
