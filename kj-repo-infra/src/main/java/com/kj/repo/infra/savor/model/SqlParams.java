package com.kj.repo.infra.savor.model;

import java.util.List;
import java.util.Map;

/**
 * @author kj
 */
public class SqlParams {
    private final ShardHolder shardHolder;
    private final StringBuilder sql;
    private final List<Map<String, Object>> paramsList;

    public SqlParams(ShardHolder shardHolder, StringBuilder sql, List<Map<String, Object>> paramsList) {
        this.shardHolder = shardHolder;
        this.sql = sql;
        this.paramsList = paramsList;
    }

    public static SqlParams model(ShardHolder shardHolder, StringBuilder sql, List<Map<String, Object>> paramsList) {
        return new SqlParams(shardHolder, sql, paramsList);
    }

    public ShardHolder getShardHolder() {
        return shardHolder;
    }

    public StringBuilder getSql() {
        return sql;
    }

    public List<Map<String, Object>> getParamsList() {
        return paramsList;
    }
}