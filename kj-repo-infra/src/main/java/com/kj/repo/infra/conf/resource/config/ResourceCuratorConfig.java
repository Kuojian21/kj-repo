package com.kj.repo.infra.conf.resource.config;

import java.util.function.Function;
import java.util.function.Supplier;

import com.kj.repo.infra.conf.Conf;
import com.kj.repo.infra.conf.curator.Curator;
import com.kj.repo.infra.conf.curator.CuratorHelper;
import com.kj.repo.infra.conf.resource.ResourceCurator;

public abstract class ResourceCuratorConfig<T> extends ResourceCurator<T> {

	@Override
	public Function<Conf<T>, Supplier<T>> function() {
		return conf -> new Curator<>(CuratorHelper.check("config", clazz()), conf, curator(),
				bytes -> ((Config<T>) conf).decode().apply(bytes));
	}

	@Override
	public Class<?> clazz() {
		return Config.class;
	}

}
