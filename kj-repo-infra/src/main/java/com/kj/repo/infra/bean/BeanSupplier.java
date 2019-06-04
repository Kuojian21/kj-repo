package com.kj.repo.infra.bean;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BeanSupplier<T> implements Supplier<T>, AutoCloseable {
    private final Supplier<T> delegate;
    private final Consumer<T> reset;
    private volatile boolean initialized;
    private T value;

    public BeanSupplier(Supplier<T> delegate) {
        this(delegate, null);
    }

    public BeanSupplier(Supplier<T> delegate, Consumer<T> reset) {
        super();
        this.delegate = delegate;
        this.reset = reset;
    }

    @Override
    public T get() {
        if (!this.initialized) {
            synchronized (this) {
                if (!this.initialized) {
                    this.value = this.delegate.get();
                    this.initialized = true;
                }
            }
        }
        return this.value;
    }

    public void reset() {
        try {
            this.close();
        } catch (Exception e) {

        } finally {
            this.initialized = false;
            this.value = null;
        }
    }

    @Override
    public void close() throws Exception {
        if (this.initialized && this.reset != null) {
            reset.accept(this.value);
        }
    }

}