package com.kj.repo.infra.conf.register;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kj.repo.infra.conf.base.Conf;
import com.kj.repo.infra.conf.resource.config.Config;
import com.kj.repo.infra.conf.resource.es.ElasticsearchHolder;
import com.kj.repo.infra.conf.resource.jedis.JedisHolder;
import com.kj.repo.infra.conf.resource.mysql.MysqlHolder;
import com.kj.repo.infra.conf.resource.oracle.OracleHolder;
import com.kj.repo.infra.conf.resource.spy.SpyHolder;

/**
 * @author kj
 * Created on 2020-09-30
 */
public class RegisterHelper {
    private static final ConcurrentMap<String, Map<Conf<?>, Supplier<?>>> REPO = Maps.newConcurrentMap();
    private static final Register register;

    static {
        List<Register> registers = Lists.newArrayList();
        ServiceLoader.load(Register.class).forEach(registers::add);
        if (registers.size() != 1) {
            throw new RuntimeException();
        }
        register = registers.get(0);
    }

    public static Supplier<JedisHolder> jedis(Conf<JedisHolder> conf) {
        return repo("jedis", conf, register::jedis);
    }

    public static Supplier<SpyHolder> spy(Conf<SpyHolder> conf) {
        return repo("spy", conf, register::spy);
    }

    public static Supplier<ElasticsearchHolder> es(Conf<ElasticsearchHolder> conf) {
        return repo("elasticsearch", conf, register::es);
    }

    public static Supplier<MysqlHolder> mysql(Conf<MysqlHolder> conf) {
        return repo("mysql", conf, register::mysql);
    }

    public static Supplier<OracleHolder> oracle(Conf<OracleHolder> conf) {
        return repo("oracle", conf, register::oracle);
    }

    public static <T> Supplier<T> config(Config<T> conf) {
        return repo("config", conf, register::config);
    }

    public static <T> Supplier<T> repo(String biz, Conf<T> conf, Function<Conf<T>, Supplier<T>> func) {
        Map<Conf<?>, Supplier<?>> confMap = REPO.computeIfAbsent(biz, key -> Maps.newConcurrentMap());
        if (!confMap.containsKey(conf)) {
            synchronized (conf) {
                if (!confMap.containsKey(conf)) {
                    confMap.put(conf, func.apply(conf));
                }
            }
        }
        return (Supplier<T>) confMap.get(conf);
    }

}
