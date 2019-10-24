package com.kj.repo.infra.conf;

public interface Conf<T> {

    T get();

    void set(T data);

    T defaultValue();

    void refresh();

    default void warmup() {
        this.get();
    }
}
