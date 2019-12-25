package com.kj.repo.infra.savor.model;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author kj
 */
public class ShardHolder {

    private final String table;
    private final NamedParameterJdbcTemplate reader;
    private final NamedParameterJdbcTemplate writer;

    public ShardHolder(String table, NamedParameterJdbcTemplate reader, NamedParameterJdbcTemplate writer) {
        this.table = table;
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ShardHolder) {
            ShardHolder o = (ShardHolder) other;
            return this.table.equals(o.table);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.table.hashCode();
    }

    public String getTable() {
        return table;
    }

    public NamedParameterJdbcTemplate getWriter() {
        return writer;
    }

    public NamedParameterJdbcTemplate getReader() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            return this.writer;
        }
        return this.reader;
    }

}