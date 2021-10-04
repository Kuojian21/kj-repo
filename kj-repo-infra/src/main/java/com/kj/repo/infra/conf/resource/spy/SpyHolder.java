package com.kj.repo.infra.conf.resource.spy;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.kj.repo.infra.conf.model.Holder;

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.MemcachedClientIF;

/**
 * @author kj
 * Created on 2020-09-30
 */
public class SpyHolder extends Holder {
    private final List<MemcachedClient> clients = Lists.newArrayList();

    private SpyHolder(SpyConfig spyConfig) {
        try {
            List<InetSocketAddress> hosts = spyConfig.getEndpoints().stream()
                    .map(endpoint -> new InetSocketAddress(endpoint.getHost(), endpoint.getPort()))
                    .collect(Collectors.toList());
            clients.add(new MemcachedClient(hosts));
            for (int i = 1; i < spyConfig.getConn(); i++) {
                clients.add(new MemcachedClient(hosts));
            }
        } catch (Exception e) {
        }
    }

    public static SpyHolder of(SpyConfig config) {
        return new SpyHolder(config);
    }

    public MemcachedClientIF get() {
        List<MemcachedClient> clients = this.clients;
        return clients.get(ThreadLocalRandom.current().nextInt(clients.size()));
    }
}
