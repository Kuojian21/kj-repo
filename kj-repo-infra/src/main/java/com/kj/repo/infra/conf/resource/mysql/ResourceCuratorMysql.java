package com.kj.repo.infra.conf.resource.mysql;

import java.util.function.Function;
import java.util.function.Supplier;
import com.kj.repo.infra.conf.Conf;
import com.kj.repo.infra.conf.curator.Curator;
import com.kj.repo.infra.conf.curator.CuratorHelper;
import com.kj.repo.infra.conf.resource.ResourceCurator;
import com.kj.repo.infra.utils.JsonUtil;

public abstract class ResourceCuratorMysql extends ResourceCurator<MysqlHolder> {

	@Override
	public Function<Conf<MysqlHolder>, Supplier<MysqlHolder>> function() {
		return conf -> new Curator<>(CuratorHelper.check("mysql", clazz()), conf, curator(),
				bytes -> MysqlHolder.of(JsonUtil.fromJSON(bytes, MysqlConfig.class)), MysqlHolder::close);
	}

	@Override
	public Class<?> clazz() {
		return MysqlConf.class;
	}

}
