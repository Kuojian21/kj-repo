package com.kj.repo.infra.conf.resource.mysql;

import java.util.concurrent.TimeUnit;

import org.apache.commons.text.StringSubstitutor;

import com.google.common.collect.ImmutableMap;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * @author kj
 * Created on 2020-10-01
 */
public class MysqlFactory {

    public static HikariDataSource get(MysqlConfig mysqlConfig, MysqlConfig.Instance i) {
        String jdbc = StringSubstitutor.replace(
                "jdbc:mysql://${host}:${port}/${database}?useUnicode=true&autoReconnectForPools=true&useCompression"
                        + "=true&rewriteBatchedStatements=true&useConfigs=maxPerformance&useSSL=false&useAffectedRows"
                        + "=true&allowMultiQueries=" + mysqlConfig.isAllowMultiQueries(),
                ImmutableMap.of("host", i.getHost(), "port", i.getPort(), "database", i.getDatabase()));

        if (mysqlConfig.getSocketTimeout() > 0) {
            jdbc = jdbc + "&socketTimeout=" + mysqlConfig.getSocketTimeout();
        }

        if (mysqlConfig.getConnectTimeout() > 0) {
            jdbc = jdbc + "&connectTimeout=" + mysqlConfig.getConnectTimeout();
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbc);
        config.setDriverClassName(com.mysql.jdbc.Driver.class.getName());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setUsername(i.getUsername());
        config.setPassword(i.getPassword());
        config.setAutoCommit(true);
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(20L));
        config.setValidationTimeout(TimeUnit.SECONDS.toMillis(5L));
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(1L));
        config.setMaximumPoolSize(100);
        config.setMinimumIdle(0);
        config.setMaxLifetime(0L);
        config.setRegisterMbeans(false);
        return new HikariDataSource(config);
    }

}
