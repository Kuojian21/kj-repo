package com.kj.repo.infra.conf.resource.es;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClient.FailureListener;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @author kj
 * Created on 2020-10-01
 */
public class ElasticsearchFactory {

    public static RestClient get(ElasticsearchConfig elasticsearchConfig) {
        return RestClient.builder(elasticsearchConfig.getEndpoints().stream()
                .map(endpoint -> new HttpHost(endpoint.getHost(), endpoint.getPort(), "http")).toArray(HttpHost[]::new))
                .setRequestConfigCallback((builder) -> builder.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(60L))
                        .setSocketTimeout((int) TimeUnit.SECONDS.toMillis(60L))
                        .setConnectionRequestTimeout((int) TimeUnit.SECONDS.toMillis(3L)))
                .setHttpClientConfigCallback((builder) -> {
                    IOReactorConfig reactorConfig =
                            IOReactorConfig.custom().setConnectTimeout((int) TimeUnit.SECONDS.toMillis(60L))
                                    .setSoTimeout((int) TimeUnit.SECONDS.toMillis(60L))
                                    .setIoThreadCount(200).build();
                    ThreadFactory threadFactory = (new ThreadFactoryBuilder()).setDaemon(true)
                            .setNameFormat("elasticsearch-rest-%d").build();
                    return builder.setDefaultIOReactorConfig(reactorConfig).setThreadFactory(threadFactory);
                }).setFailureListener(new FailureListener() {
                    public void onFailure(Node node) {

                    }
                }).build();
    }
}
