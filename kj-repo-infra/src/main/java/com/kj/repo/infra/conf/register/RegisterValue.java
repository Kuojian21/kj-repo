package com.kj.repo.infra.conf.register;

import java.util.function.Supplier;

import com.kj.repo.infra.conf.base.Conf;
import com.kj.repo.infra.conf.resource.es.ElasticsearchHolder;
import com.kj.repo.infra.conf.resource.jedis.JedisHolder;
import com.kj.repo.infra.conf.resource.mysql.MysqlHolder;
import com.kj.repo.infra.conf.resource.oracle.OracleHolder;
import com.kj.repo.infra.conf.resource.spy.SpyHolder;

/**
 * @author kj
 * Created on 2020-10-02
 */
public class RegisterValue implements Register {
    @Override
    public Supplier<JedisHolder> jedis(Conf<JedisHolder> conf) {
        return conf.defValue();
    }

    @Override
    public Supplier<SpyHolder> spy(Conf<SpyHolder> conf) {
        return conf.defValue();
    }

    @Override
    public Supplier<ElasticsearchHolder> es(Conf<ElasticsearchHolder> conf) {
        return conf.defValue();
    }

    @Override
    public Supplier<MysqlHolder> mysql(Conf<MysqlHolder> conf) {
        return conf.defValue();
    }

    @Override
    public Supplier<OracleHolder> oracle(Conf<OracleHolder> conf) {
        return conf.defValue();
    }

    @Override
    public <T> Supplier<T> config(Conf<T> conf) {
        return conf.defValue();
    }
}
