package com.kj.repo.infra.conf.resource.oracle;

import com.kj.repo.infra.conf.Conf;
import com.kj.repo.infra.conf.resource.ResourceRepository;

/**
 * @author kj
 * Created on 2020-09-30
 */
public interface OracleConf extends Conf<OracleHolder> {
    @Override
    default OracleHolder get() {
    	return ResourceRepository.get(OracleConf.class, this).get();
        
    }
}
