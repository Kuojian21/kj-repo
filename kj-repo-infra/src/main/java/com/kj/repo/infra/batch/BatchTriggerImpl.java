package com.kj.repo.infra.batch;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.google.common.util.concurrent.MoreExecutors;
import com.kj.repo.infra.logger.LoggerHelper;

/**
 * @author kj
 */
public class BatchTriggerImpl<E, T> implements BatchTrigger<E> {

    private static final Logger logger = LoggerHelper.getLogger();

    private final Consumer<List<T>> consumer;
    private final int batchsize;
    private final long linger;
    private final BatchBuffer<E, T> buffer;
    private final BiConsumer<Throwable, List<T>> throwableHandler;
    private final ScheduledExecutorService scheduledExecutor;
    private final Executor workerExecutor;
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicBoolean running = new AtomicBoolean();

    BatchTriggerImpl(Consumer<List<T>> consumer, int batchsize, long linger, BatchBuffer<E, T> buffer,
            BiConsumer<Throwable, List<T>> throwableHandler, ScheduledExecutorService scheduledExecutor,
            Executor workerExecutor) {
        this.consumer = consumer;
        this.batchsize = batchsize;
        this.linger = linger;
        this.buffer = buffer;
        this.throwableHandler = throwableHandler != null ? throwableHandler : (t, d) -> logger.error("", t);
        this.scheduledExecutor = scheduledExecutor;
        this.workerExecutor = workerExecutor != null ? workerExecutor : MoreExecutors.directExecutor();
        this.scheduledExecutor.schedule(new BatchConsumerRunnable(), this.linger, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                doBatchConsumer(BatchTriggerImpl.TriggerType.MANUALLY);
            }
        });
    }

    @Override
    public void enqueue(E element) {
        try {
            buffer.add(element);
            tryTrigBatchConsume();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void trigger() {
        doBatchConsumer(BatchTriggerImpl.TriggerType.MANUALLY);
    }

    private void tryTrigBatchConsume() {
        if (buffer.size() >= batchsize && lock.tryLock()) {
            try {
                if (buffer.size() >= batchsize) {
                    if (!running.get()) {// prevent repeat enqueue
                        scheduledExecutor.execute(() -> doBatchConsumer(TriggerType.ENQUEUE));
                        running.set(true);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private void doBatchConsumer(TriggerType type) {
        lock.lock();
        try {
            running.set(true);
            int beforeBufferSize = buffer.size();
            int consumedSize = 0;
            while (!buffer.isEmpty()) {
                if (buffer.size() < batchsize) {
                    if (type == TriggerType.ENQUEUE) {
                        return;
                    } else if (type == TriggerType.LINGER && consumedSize >= beforeBufferSize) {
                        return;
                    }
                }
                List<T> data = buffer.drainTo(batchsize);
                logger.debug("do batch consumer,type:{}, size:{}", type, data.size());
                if (!data.isEmpty()) {
                    consumedSize += data.size();
                    doConsume(data);
                }
            }
        } finally {
            running.set(false);
            lock.unlock();
        }
    }

    private void doConsume(List<T> data) {
        this.workerExecutor.execute(() -> {
            try {
                consumer.accept(data);
            } catch (Throwable e) {
                throwableHandler.accept(e, data);
            }
        });
    }

    private enum TriggerType {
        LINGER,
        ENQUEUE,
        MANUALLY
    }

    private class BatchConsumerRunnable implements Runnable {
        @Override
        public void run() {
            try {
                doBatchConsumer(TriggerType.LINGER);
            } finally {
                scheduledExecutor.schedule(this, linger, TimeUnit.MILLISECONDS);
            }
        }
    }

}
