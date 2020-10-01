package com.kj.repo.infra.conf.resource.es;

import com.kj.repo.infra.conf.base.Conf;
import com.kj.repo.infra.conf.register.RegisterHelper;

/**
 * @author kj
 * Created on 2020-09-30
 */
public interface ElasticsearchConf extends Conf<ElasticsearchHolder> {
    @Override
    default ElasticsearchHolder get() {
        return RegisterHelper.es(this).get();
    }
}
