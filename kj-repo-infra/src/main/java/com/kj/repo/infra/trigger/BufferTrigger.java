package com.kj.repo.infra.trigger;

public interface BufferTrigger<E> {

    static <E, T> BatchBufferTriggerBuilder<E, T> builder() {
        return new BatchBufferTriggerBuilder<>();
    }

    void enqueue(E element);

    void trigger();
}
