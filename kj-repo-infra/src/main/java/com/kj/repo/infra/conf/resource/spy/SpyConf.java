package com.kj.repo.infra.conf.resource.spy;

import com.kj.repo.infra.conf.base.Conf;
import com.kj.repo.infra.conf.register.RegisterHelper;

/**
 * @author kj
 * Created on 2020-09-30
 */
public interface SpyConf extends Conf<SpyHolder> {
    @Override
    default SpyHolder get() {
        return RegisterHelper.spy(this).get();
    }
}
