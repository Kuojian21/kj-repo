package com.kj.repo.infra.conf.resource.mysql;

import java.util.List;

import com.kj.repo.infra.conf.model.Endpoint;

/**
 * @author kj
 * Created on 2020-10-01
 */
public class MysqlConfig {
    private int socketTimeout;
    private long connectTimeout;
    private boolean allowMultiQueries;
    private List<Instance> masters;
    private List<Instance> slaves;

    public List<Instance> getMasters() {
        return masters;
    }

    public void setMasters(List<Instance> masters) {
        this.masters = masters;
    }

    public List<Instance> getSlaves() {
        return slaves;
    }

    public void setSlaves(List<Instance> slaves) {
        this.slaves = slaves;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public boolean isAllowMultiQueries() {
        return allowMultiQueries;
    }

    public void setAllowMultiQueries(boolean allowMultiQueries) {
        this.allowMultiQueries = allowMultiQueries;
    }

    public static class Instance extends Endpoint {
        private String database;
        private String username;
        private String password;

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
