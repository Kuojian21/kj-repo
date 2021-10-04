package com.kj.repo.infra.conf.resource.jedis;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.kj.repo.infra.conf.model.Holder;
import com.kj.repo.infra.utils.EnhancerUtil;

import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Response;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.ShardedJedisPool;

/**
 * @author kj
 * Created on 2020-09-30
 */
public class JedisHolder extends Holder {
    private final ShardedJedisPool pool;

    private JedisHolder(JedisConfig jedisConfig) {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(100);
        poolConfig.setMaxIdle(100);
        poolConfig.setMinIdle(0);
        poolConfig.setMaxWaitMillis(SECONDS.toMillis(2));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setMinEvictableIdleTimeMillis(MINUTES.toMillis(10));
        poolConfig.setTimeBetweenEvictionRunsMillis(MINUTES.toMillis(1));
        poolConfig.setJmxEnabled(false);
        pool = new ShardedJedisPool(poolConfig, jedisConfig.getInstances().stream()
                .map(shard -> {
                    JedisShardInfo info =
                            new JedisShardInfo(shard.getHost(), shard.getPort(), shard.getConnectTimeout(),
                                    shard.getSoTimeout(), shard.getWeight());
                    info.setPassword(shard.getPassword());
                    return info;
                }).collect(Collectors.toList()));
    }

    public static JedisHolder of(JedisConfig config) {
        return new JedisHolder(config);
    }

    public JedisCommands jedis() {
        return EnhancerUtil.enhancer(JedisCommands.class, (method, args) -> {
            try (ShardedJedis jedis = pool.getResource()) {
                return method.invoke(jedis, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <K, V> List<Response<V>> pipeline(Collection<K> keys,
            BiFunction<ShardedJedisPipeline, K, Response<V>> function) {
        try (ShardedJedis shardedJedis = pool.getResource()) {
            ShardedJedisPipeline pipeline = shardedJedis.pipelined();
            List<Response<V>> rtn =
                    keys.stream().map(key -> function.apply(pipeline, key)).collect(Collectors.toList());
            pipeline.sync();
            return rtn;
        }
    }

    public void pipeline(Consumer<ShardedJedisPipeline> consumer) {
        try (ShardedJedis shardedJedis = pool.getResource()) {
            ShardedJedisPipeline pipeline = shardedJedis.pipelined();
            consumer.accept(pipeline);
            pipeline.sync();
        }
    }
}
