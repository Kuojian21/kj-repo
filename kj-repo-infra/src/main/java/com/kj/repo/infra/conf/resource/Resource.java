package com.kj.repo.infra.conf.resource;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.curator.shaded.com.google.common.collect.Maps;

import com.kj.repo.infra.conf.Conf;

public abstract class Resource<T> {

	private final ConcurrentMap<Conf<T>, Supplier<T>> resources = Maps.newConcurrentMap();

	public final Supplier<T> get(Conf<T> conf) {
		Supplier<T> supplier = resources.get(conf);
		if (supplier == null) {
			synchronized (this) {
				supplier = resources.get(conf);
				if (supplier == null) {
					supplier = this.function().apply(conf);
					resources.put(conf, this.function().apply(conf));
				}
			}
		}
		return supplier;
	}

	public abstract Function<Conf<T>, Supplier<T>> function();

	public abstract Class<?> clazz();
}
