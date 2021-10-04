package com.kj.repo.infra.conf.resource.jedis;

import java.util.function.Function;
import java.util.function.Supplier;
import com.kj.repo.infra.conf.Conf;
import com.kj.repo.infra.conf.curator.Curator;
import com.kj.repo.infra.conf.curator.CuratorHelper;
import com.kj.repo.infra.conf.resource.ResourceCurator;
import com.kj.repo.infra.utils.JsonUtil;

public abstract class ResourceCuratorJedis extends ResourceCurator<JedisHolder> {

	@Override
	public Function<Conf<JedisHolder>, Supplier<JedisHolder>> function() {
		return conf -> new Curator<>(CuratorHelper.check("jedis", clazz()), conf, curator(),
				bytes -> JedisHolder.of(JsonUtil.fromJSON(bytes, JedisConfig.class)), JedisHolder::close);
	}

	@Override
	public Class<?> clazz() {
		return JedisConf.class;
	}

}
