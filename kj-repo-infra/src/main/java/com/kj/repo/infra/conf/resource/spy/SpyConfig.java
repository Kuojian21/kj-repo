package com.kj.repo.infra.conf.resource.spy;

import java.util.List;

import com.kj.repo.infra.conf.base.Endpoint;

/**
 * @author kj
 * Created on 2020-10-01
 */
public class SpyConfig {
    private int conn = 1;
    private List<Endpoint> endpoints;

    public SpyConfig(int conn, List<Endpoint> endpoints) {
        this.conn = conn;
        this.endpoints = endpoints;
    }

    public int getConn() {
        return conn;
    }

    public void setConn(int conn) {
        this.conn = conn;
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }
}
