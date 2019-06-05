package com.kj.repo.infra.bean;

import java.util.function.Supplier;

/**
 * @param <T>
 * @author kuojian21
 */
public class BeanSupplier<T> implements Supplier<T>, AutoCloseable {
    private final Supplier<T> delegate;
    private final Consumer<T> close;
    private volatile boolean initialized;
    private T value;

    public BeanSupplier(Supplier<T> delegate) {
        this(delegate, null);
    }

    public BeanSupplier(Supplier<T> delegate, Consumer<T> close) {
        super();
        this.delegate = delegate;
        this.close = close;
    }

    @Override
    public T get() {
        if (!this.initialized) {
            synchronized (this) {
                if (!this.initialized) {
                    try {
                        this.value = this.delegate.get();
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                    this.initialized = true;
                }
            }
        }
        return this.value;
    }

    public void reset() throws Exception {
        if (this.initialized) {
            if (this.close != null) {
                close.accept(this.value);
            }
            this.value = this.delegate.get();
        }
    }

    @Override
    public void close() throws Exception {
        if (this.initialized && this.close != null) {
            close.accept(this.value);
        }
        this.initialized = false;
        this.value = null;
    }

    @FunctionalInterface
    public interface Supplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T t) throws Exception;
    }

}