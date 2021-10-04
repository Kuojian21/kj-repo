package com.kj.repo.infra.conf.resource.es;

import java.util.function.Function;
import java.util.function.Supplier;

import com.kj.repo.infra.conf.Conf;
import com.kj.repo.infra.conf.curator.Curator;
import com.kj.repo.infra.conf.curator.CuratorHelper;
import com.kj.repo.infra.conf.resource.ResourceCurator;
import com.kj.repo.infra.utils.JsonUtil;

public abstract class ResourceCuratorElasticsearch extends ResourceCurator<ElasticsearchHolder>{

	@Override
	public Function<Conf<ElasticsearchHolder>, Supplier<ElasticsearchHolder>> function() {
		return conf-> new Curator<>(CuratorHelper.check("elasticsearch", clazz()), conf, curator(),
                bytes -> ElasticsearchHolder.of(JsonUtil.fromJSON(bytes, ElasticsearchConfig.class)),
                ElasticsearchHolder::close);
	}

	@Override
	public Class<?> clazz() {
		return ElasticsearchConf.class;
	}

}
