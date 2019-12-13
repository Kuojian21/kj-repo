package com.kj.repo.infra.base;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.google.common.base.Supplier;

public class LocalSupplier<T> {

    private static LocalSupplier<ExecutorService> asyncExecutor = new LocalSupplier<>(
            () -> Executors.newFixedThreadPool(2));

    private final Supplier<T> inner;
    private final Consumer<T> release;
    private final T defaultValue;
    private final boolean async;
    private volatile boolean inited = false;
    private volatile T value;

    public LocalSupplier(Supplier<T> inner) {
        this(inner, null, null, false);
    }

    public LocalSupplier(Supplier<T> inner, Consumer<T> release) {
        this(inner, release, null, false);
    }

    public LocalSupplier(Supplier<T> inner, T defaultValue) {
        this(inner, null, defaultValue, false);
    }

    public LocalSupplier(Supplier<T> inner, Consumer<T> release, T defaultValue) {
        this(inner, release, defaultValue, false);
    }

    public LocalSupplier(Supplier<T> inner, Consumer<T> release, T defaultValue, boolean async) {
        super();
        this.inner = inner;
        this.release = release;
        this.defaultValue = defaultValue;
        this.async = async;
    }

    public T get() {
        if (!this.inited) {
            synchronized (this) {
                if (!this.inited) {
                    this.value = this.inner.get();
                    this.inited = true;
                }
            }
        }
        return Optional.ofNullable(this.value).orElse(this.defaultValue);
    }

    public void refresh() {
        Runnable runnable = () -> {
            T oValue = this.value;
            this.value = this.inner.get();
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

    public void release() {
        T oValue = this.value;
        this.inited = false;
        this.value = null;
        if (this.release != null && oValue != null) {
            this.release.accept(oValue);
        }
    }
}
