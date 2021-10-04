package com.kj.repo.infra.conf.resource.es;

import com.kj.repo.infra.conf.Conf;
import com.kj.repo.infra.conf.resource.ResourceRepository;

/**
 * @author kj
 * Created on 2020-09-30
 */
public interface ElasticsearchConf extends Conf<ElasticsearchHolder> {
    @Override
    default ElasticsearchHolder get() {
    	return ResourceRepository.get(ElasticsearchHolder.class, this).get();
    }
}
