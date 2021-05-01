package com.kj.repo.infra.conf.resource.jedis;

import java.util.List;

import com.kj.repo.infra.conf.model.Endpoint;

/**
 * @author kj
 * Created on 2020-10-01
 */
public class JedisConfig {
    private List<Instance> instances;

    public List<Instance> getInstances() {
        return instances;
    }

    public void setInstances(List<Instance> instances) {
        this.instances = instances;
    }

    public static class Instance extends Endpoint {
        private int connectTimeout;
        private int soTimeout;
        private int database;
        private String password;
        private int weight;

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getSoTimeout() {
            return soTimeout;
        }

        public void setSoTimeout(int soTimeout) {
            this.soTimeout = soTimeout;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }
    }
}
