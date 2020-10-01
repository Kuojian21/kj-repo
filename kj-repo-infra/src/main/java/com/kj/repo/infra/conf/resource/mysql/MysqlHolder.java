package com.kj.repo.infra.conf.resource.mysql;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.kj.repo.infra.conf.base.Holder;

/**
 * @author kj
 * Created on 2020-09-30
 */
public class MysqlHolder extends Holder {
    private final List<NamedParameterJdbcTemplate> masters;
    private final List<NamedParameterJdbcTemplate> slaves;
    public MysqlHolder(MysqlConfig mysqlConfig) {
        this.masters = mysqlConfig.getMasters().stream()
                .map(instance -> new NamedParameterJdbcTemplate(MysqlFactory.get(mysqlConfig, instance))).collect(
                        Collectors.toList());
        this.slaves = mysqlConfig.getSlaves().stream()
                .map(instance -> new NamedParameterJdbcTemplate(MysqlFactory.get(mysqlConfig, instance))).collect(
                        Collectors.toList());
    }

    public static MysqlHolder of(MysqlConfig config) {
        return new MysqlHolder(config);
    }
}
