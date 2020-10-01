package com.kj.repo.infra.conf.resource.mysql;

import com.kj.repo.infra.conf.base.Conf;
import com.kj.repo.infra.conf.register.RegisterHelper;

/**
 * @author kj
 * Created on 2020-09-30
 */
public interface MysqlConf extends Conf<MysqlHolder> {

    @Override
    default MysqlHolder get() {
        return RegisterHelper.mysql(this).get();
    }
}
