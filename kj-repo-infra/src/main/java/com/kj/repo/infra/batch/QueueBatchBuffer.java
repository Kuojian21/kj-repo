package com.kj.repo.infra.batch;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

/**
 * @author kj
 */
public class QueueBatchBuffer<E, T> implements BatchBuffer<E, T> {

    private final BlockingQueue<E> queue;
    private final Function<E, T> mapper;

    public QueueBatchBuffer(int capacity, Function<E, T> mapper) {
        this.queue = new LinkedBlockingQueue<>(capacity);
        this.mapper = mapper;
    }

    @Override
    public void add(E element) throws InterruptedException {
        queue.put(element);
    }

    @Override
    public List<T> drainTo(int batchsize) {
        List<E> data = Lists.newArrayList();
        queue.drainTo(data, batchsize);
        return data.stream().map(mapper).collect(Collectors.toList());
    }

    @Override
    public int size() {
        return 0;
    }
}
