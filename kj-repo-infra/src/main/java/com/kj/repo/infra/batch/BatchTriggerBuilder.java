package com.kj.repo.infra.batch;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kj.repo.infra.base.builder.Builder;
import com.kj.repo.infra.batch.buffer.Buffer;

/**
 * @author kj
 */
public class BatchTriggerBuilder<E, T> implements Builder<BatchTriggerImpl<E, T>> {

    private static final int DEFAULT_BATCHSIZE = 100;
    private static final long DEFAULT_LINGER = 1000;

    private Consumer<List<T>> consumer;
    private int batchsize = DEFAULT_BATCHSIZE;
    private long linger = DEFAULT_LINGER;
    private Buffer<E, T> buffer;
    private BiConsumer<Throwable, List<T>> throwableHandler;
    private ScheduledExecutorService scheduledExecutor;
    private Executor workerExecutor;

    public BatchTriggerBuilder<E, T> setConsumer(Consumer<List<T>> consumer) {
        this.consumer = consumer;
        return this;
    }

    public BatchTriggerBuilder<E, T> setBatchsize(int batchsize) {
        this.batchsize = batchsize;
        return this;
    }

    public BatchTriggerBuilder<E, T> setLinger(long linger) {
        this.linger = linger;
        return this;
    }

    public BatchTriggerBuilder<E, T> setBuffer(Buffer<E, T> buffer) {
        this.buffer = buffer;
        return this;
    }

    public BatchTriggerBuilder<E, T> setThrowableHandler(BiConsumer<Throwable, List<T>> throwableHandler) {
        this.throwableHandler = throwableHandler;
        return this;
    }

    public BatchTriggerBuilder<E, T> setScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
        this.scheduledExecutor = scheduledExecutor;
        return this;
    }

    public BatchTriggerBuilder<E, T> setWorkerExecutor(Executor workerExecutor) {
        this.workerExecutor = workerExecutor;
        return this;
    }

    @Override
    public BatchTriggerImpl<E, T> doBuild() {
        return new BatchTriggerImpl<>(consumer, batchsize, linger, buffer, throwableHandler, scheduledExecutor,
                workerExecutor);
    }

    @Override
    public void ensure() {
        if (this.scheduledExecutor == null) {
            this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor(
                    new ThreadFactoryBuilder().setNameFormat("batch-buffer-batch-%d").setDaemon(true).build());
        }
    }
}
