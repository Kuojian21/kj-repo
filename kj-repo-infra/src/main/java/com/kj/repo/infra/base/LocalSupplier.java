package com.kj.repo.infra.base;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.google.common.base.Supplier;

/**
 * @author kj
 */
public class LocalSupplier<T> {

    private static final LocalSupplier<ExecutorService> asyncExecutor = new LocalSupplier<>(
            () -> Executors.newFixedThreadPool(1));

    private final Supplier<T> delegate;
    private final Consumer<T> release;
    private final Supplier<T> defValue;
    private final boolean async;
    private volatile boolean inited = false;
    private volatile T value;

    public LocalSupplier(Supplier<T> delegate) {
        this(delegate, null, () -> null, false);
    }

    public LocalSupplier(Supplier<T> delegate, Consumer<T> release) {
        this(delegate, release, () -> null, false);
    }

    public LocalSupplier(Supplier<T> delegate, Supplier<T> defValue) {
        this(delegate, null, defValue, false);
    }

    public LocalSupplier(Supplier<T> delegate, Consumer<T> release, Supplier<T> defValue) {
        this(delegate, release, defValue, false);
    }

    public LocalSupplier(Supplier<T> delegate, Consumer<T> release, Supplier<T> defValue, boolean async) {
        super();
        this.delegate = delegate;
        this.release = release;
        this.defValue = defValue;
        this.async = async;
    }

    public T get() {
        if (!this.inited) {
            synchronized (this) {
                if (!this.inited) {
                    this.value = this.delegate.get();
                    this.inited = true;
                }
            }
        }
        return Optional.ofNullable(this.value).orElseGet(this.defValue);
    }

    public void refresh() {
        Runnable runnable = () -> {
            T oValue = this.value;
            this.value = this.delegate.get();
            if (this.release != null && oValue != null) {
                this.release.accept(oValue);
            }
        };
        if (async) {
            asyncExecutor.get().execute(runnable);
        } else {
            runnable.run();
        }
    }

    public void warm() {
        this.get();
    }

    public void release() {
        T oValue = this.value;
        this.inited = false;
        this.value = null;
        if (this.release != null && oValue != null) {
            this.release.accept(oValue);
        }
    }
}