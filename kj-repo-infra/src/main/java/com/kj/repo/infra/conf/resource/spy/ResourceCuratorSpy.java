package com.kj.repo.infra.conf.resource.spy;

import java.util.function.Function;
import java.util.function.Supplier;
import com.kj.repo.infra.conf.Conf;
import com.kj.repo.infra.conf.curator.Curator;
import com.kj.repo.infra.conf.curator.CuratorHelper;
import com.kj.repo.infra.conf.resource.ResourceCurator;
import com.kj.repo.infra.utils.JsonUtil;

public abstract class ResourceCuratorSpy extends ResourceCurator<SpyHolder> {

	@Override
	public Function<Conf<SpyHolder>, Supplier<SpyHolder>> function() {
		return conf -> new Curator<>(CuratorHelper.check("spy", clazz()), conf, curator(),
				bytes -> SpyHolder.of(JsonUtil.fromJSON(bytes, SpyConfig.class)), SpyHolder::close);
	}

	@Override
	public Class<?> clazz() {
		return SpyConf.class;
	}

}
