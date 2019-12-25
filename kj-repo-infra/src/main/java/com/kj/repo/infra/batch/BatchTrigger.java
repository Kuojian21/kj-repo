package com.kj.repo.infra.batch;

/**
 * @author kj
 */
public interface BatchTrigger<E> {

    static <E, T> BatchTriggerBuilder<E, T> builder() {
        return new BatchTriggerBuilder<>();
    }

    void enqueue(E element);

    void trigger();
}
