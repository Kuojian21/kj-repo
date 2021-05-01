package com.kj.repo.infra.conf.register;

import java.util.function.Supplier;

import com.kj.repo.infra.conf.Conf;
import com.kj.repo.infra.conf.zk.Curator;
import com.kj.repo.infra.conf.resource.config.Config;
import com.kj.repo.infra.conf.resource.es.ElasticsearchConfig;
import com.kj.repo.infra.conf.resource.es.ElasticsearchHolder;
import com.kj.repo.infra.conf.resource.jedis.JedisConfig;
import com.kj.repo.infra.conf.resource.jedis.JedisHolder;
import com.kj.repo.infra.conf.resource.mysql.MysqlConfig;
import com.kj.repo.infra.conf.resource.mysql.MysqlHolder;
import com.kj.repo.infra.conf.resource.oracle.OracleConfig;
import com.kj.repo.infra.conf.resource.oracle.OracleHolder;
import com.kj.repo.infra.conf.resource.spy.SpyConfig;
import com.kj.repo.infra.conf.resource.spy.SpyHolder;
import com.kj.repo.infra.curator.CuratorHelper;
import com.kj.repo.infra.serializer.json.JsonHelper;

/**
 * @author kj
 * Created on 2020-10-02
 */
public class RegisterCurator implements Register {
    @Override
    public Supplier<JedisHolder> jedis(Conf<JedisHolder> conf) {
        return new Curator<>("jedis", conf, CuratorHelper.curator(), bytes -> JedisHolder.of(JsonHelper.fromJSON(bytes,
                JedisConfig.class)), JedisHolder::close);
    }

    @Override
    public Supplier<SpyHolder> spy(Conf<SpyHolder> conf) {
        return new Curator<>("spy", conf, CuratorHelper.curator(), bytes -> SpyHolder.of(JsonHelper.fromJSON(bytes,
                SpyConfig.class)), SpyHolder::close);
    }

    @Override
    public Supplier<ElasticsearchHolder> es(Conf<ElasticsearchHolder> conf) {
        return new Curator<>("spy", conf, CuratorHelper.curator(),
                bytes -> ElasticsearchHolder.of(JsonHelper.fromJSON(bytes, ElasticsearchConfig.class)),
                ElasticsearchHolder::close);
    }

    @Override
    public Supplier<MysqlHolder> mysql(Conf<MysqlHolder> conf) {
        return new Curator<>("spy", conf, CuratorHelper.curator(),
                bytes -> MysqlHolder.of(JsonHelper.fromJSON(bytes, MysqlConfig.class)), MysqlHolder::close);
    }

    @Override
    public Supplier<OracleHolder> oracle(Conf<OracleHolder> conf) {
        return new Curator<>("spy", conf, CuratorHelper.curator(),
                bytes -> OracleHolder.of(JsonHelper.fromJSON(bytes, OracleConfig.class)), OracleHolder::close);
    }

    @Override
    public <T> Supplier<T> config(Conf<T> conf) {
        Config<T> config = (Config<T>) conf;
        return new Curator<>("config", conf, CuratorHelper.curator(), bytes -> config.decode().apply(bytes), i -> {});
    }
}
