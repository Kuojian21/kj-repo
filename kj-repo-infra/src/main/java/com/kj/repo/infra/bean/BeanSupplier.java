package com.kj.repo.infra.bean;

import java.util.function.Supplier;

/**
 * @param <T>
 * @author kuojian21
 */
public class BeanSupplier<T> implements Supplier<T>, AutoCloseable {
    private final Supplier<T> delegate;
    private final Consumer<T> clean;
    private volatile boolean initialized;
    private volatile T value;

    public BeanSupplier(Supplier<T> delegate) {
        this(delegate, null);
    }

    public BeanSupplier(Supplier<T> delegate, Consumer<T> clean) {
        super();
        this.delegate = delegate;
        this.clean = clean;
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
        T oValue = this.value;
        this.value = this.delegate.get();
        if (this.clean != null && oValue != null) {
            this.clean.accept(oValue);
        }
    }

    @Override
    public void close() throws Exception {
        if (this.clean != null && this.value != null) {
            this.clean.accept(this.value);
        }
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