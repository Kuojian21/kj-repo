package com.kj.repo.infra.conf.resource.jedis;

import com.kj.repo.infra.conf.Conf;
import com.kj.repo.infra.conf.register.RegisterHelper;

/**
 * @author kj
 * Created on 2020-09-30
 */
public interface JedisConf extends Conf<JedisHolder> {
    @Override
    default JedisHolder get() {
        return RegisterHelper.jedis(this).get();
    }
}
