package com.kj.repo.infra;

import java.util.Optional;
import java.util.function.Consumer;

import com.google.common.base.Supplier;

/**
 * @author kj
 */
public class LocalSupplier<T> implements Supplier<T> {

	private final Supplier<T> delegate;
	private final Consumer<T> release;
	private final Supplier<T> defValue;
	private volatile boolean inited = false;
	private volatile T value;

	public LocalSupplier(Supplier<T> delegate) {
		this(delegate, null, () -> null);
	}

	public LocalSupplier(Supplier<T> delegate, Consumer<T> release) {
		this(delegate, release, () -> null);
	}

	public LocalSupplier(Supplier<T> delegate, Supplier<T> defValue) {
		this(delegate, null, defValue);
	}

	public LocalSupplier(Supplier<T> delegate, Consumer<T> release, Supplier<T> defValue) {
		super();
		this.delegate = delegate;
		this.release = release;
		this.defValue = defValue;
	}

	@Override
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
		T oValue = this.value;
		this.value = this.delegate.get();
		if (this.release != null && oValue != null) {
			this.release.accept(oValue);
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