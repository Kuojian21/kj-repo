package com.kj.repo.infra.conf.resource.mysql;

import com.kj.repo.infra.conf.Conf;
import com.kj.repo.infra.conf.resource.ResourceRepository;
import com.kj.repo.infra.conf.resource.es.ElasticsearchHolder;

/**
 * @author kj
 * Created on 2020-09-30
 */
public interface MysqlConf extends Conf<MysqlHolder> {

    @Override
    default MysqlHolder get() {
    	return ResourceRepository.get(ElasticsearchHolder.class, this).get();
    }
}
