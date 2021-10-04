package com.kj.repo.infra.conf.resource.spy;

import com.kj.repo.infra.conf.Conf;
import com.kj.repo.infra.conf.resource.ResourceRepository;
import com.kj.repo.infra.conf.resource.oracle.OracleConf;

/**
 * @author kj
 * Created on 2020-09-30
 */
public interface SpyConf extends Conf<SpyHolder> {
    @Override
    default SpyHolder get() {
    	return ResourceRepository.get(SpyConf.class, this).get();
    }
}
