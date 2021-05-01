package com.kj.repo.infra.conf.resource.es;

import org.elasticsearch.client.RestClient;

import com.kj.repo.infra.conf.model.Holder;

/**
 * @author kj
 * Created on 2020-09-30
 */
public class ElasticsearchHolder extends Holder {

    private final RestClient restClient;

    public ElasticsearchHolder(ElasticsearchConfig elasticsearchConfig) {
        restClient = ElasticsearchFactory.get(elasticsearchConfig);
    }

    public static ElasticsearchHolder of(ElasticsearchConfig config) {
        return new ElasticsearchHolder(config);
    }


}
