package com.kj.repo.infra.conf.resource.es;

import java.util.List;

import com.kj.repo.infra.conf.base.Endpoint;

/**
 * @author kj
 * Created on 2020-10-01
 */
public class ElasticsearchConfig {
    private List<Endpoint> endpoints;

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }
}
