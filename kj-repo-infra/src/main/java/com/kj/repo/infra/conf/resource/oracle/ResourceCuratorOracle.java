package com.kj.repo.infra.conf.resource.oracle;

import java.util.function.Function;
import java.util.function.Supplier;
import com.kj.repo.infra.conf.Conf;
import com.kj.repo.infra.conf.curator.Curator;
import com.kj.repo.infra.conf.curator.CuratorHelper;
import com.kj.repo.infra.conf.resource.ResourceCurator;
import com.kj.repo.infra.utils.JsonUtil;

public abstract class ResourceCuratorOracle extends ResourceCurator<OracleHolder> {

	@Override
	public Function<Conf<OracleHolder>, Supplier<OracleHolder>> function() {
		return conf -> new Curator<>(CuratorHelper.check("oracle", clazz()), conf, curator(),
				bytes -> OracleHolder.of(JsonUtil.fromJSON(bytes, OracleConfig.class)), OracleHolder::close);
	}

	@Override
	public Class<?> clazz() {
		return OracleConf.class;
	}
}
