package com.kj.repo.infra.trigger;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.kj.repo.infra.builder.Builder;

public class BatchBufferTriggerBuilder<E, T> implements Builder<BatchBufferTrigger<E, T>> {

    private static final int DEFAULT_BATCHSIZE = 100;
    private static final long DEFAULT_LINGER = 1000;

    private Consumer<List<T>> consumer;
    private int batchsize = DEFAULT_BATCHSIZE;
    private long linger = DEFAULT_LINGER;
    private BatchBuffer<E, T> buffer;
    private BiConsumer<Throwable, List<T>> throwableHandler;
    private ScheduledExecutorService scheduledExecutor;
    private Executor workerExecutor;

    public BatchBufferTriggerBuilder<E, T> setConsumer(Consumer<List<T>> consumer) {
        this.consumer = consumer;
        return this;
    }

    public BatchBufferTriggerBuilder<E, T> setBatchsize(int batchsize) {
        this.batchsize = batchsize;
        return this;
    }

    public BatchBufferTriggerBuilder<E, T> setLinger(long linger) {
        this.linger = linger;
        return this;
    }

    public BatchBufferTriggerBuilder<E, T> setBuffer(BatchBuffer<E, T> buffer) {
        this.buffer = buffer;
        return this;
    }

    public BatchBufferTriggerBuilder<E, T> setBuffer(BlockingQueue<E> queue, Function<E, T> mapper) {
        this.buffer = new BatchBuffer<E, T>() {

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
        };
        return this;
    }

    public BatchBufferTriggerBuilder<E, T> setThrowableHandler(BiConsumer<Throwable, List<T>> throwableHandler) {
        this.throwableHandler = throwableHandler;
        return this;
    }

    public BatchBufferTriggerBuilder<E, T> setScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
        this.scheduledExecutor = scheduledExecutor;
        return this;
    }

    public BatchBufferTriggerBuilder<E, T> setWorkerExecutor(Executor workerExecutor) {
        this.workerExecutor = workerExecutor;
        return this;
    }

    @Override
    public BatchBufferTrigger<E, T> build() {
        return new BatchBufferTrigger<>(consumer, batchsize, linger, buffer, throwableHandler, scheduledExecutor, workerExecutor);
    }
}
