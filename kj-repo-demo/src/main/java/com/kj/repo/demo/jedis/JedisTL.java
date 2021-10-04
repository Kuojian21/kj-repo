package com.kj.repo.demo.jedis;

import java.io.Closeable;

import com.kj.repo.infra.Consumer;
import com.kj.repo.infra.Function;
import com.kj.repo.infra.utils.EnhancerUtil;
import com.kj.repo.infra.utils.RunUtil;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.util.Pool;

/**
 * @author kj
 * Created on 2020-03-15
 */
public class JedisTL<T extends Closeable> {

    private final Pool<T> pool;
    private final Class<T> clazz;

    public JedisTL(Pool<T> pool, Class<T> clazz) {
        this.pool = pool;
        this.clazz = clazz;
    }

    public static JedisTL<Jedis> jedisTL(JedisPool jedisPool) {
        return new JedisTL<>(jedisPool, Jedis.class);
    }


    public static JedisTL<Jedis> jedisTL(JedisSentinelPool jedisSentinelPool) {
        return new JedisTL<>(jedisSentinelPool, Jedis.class);
    }

    public static JedisTL<ShardedJedis> jedisTL(ShardedJedisPool shardedJedisPool) {
        return new JedisTL<>(shardedJedisPool, ShardedJedis.class);
    }

    public T jedis() {
        return EnhancerUtil.enhancer(clazz, (method, args) ->
                RunUtil.run(() -> this.execute(f -> {
                            return method.invoke(f, args);
                        })
                )
        );
    }

    public final <R> R execute(Function<T, R> function) throws Exception {
        T t = null;
        try {
            t = this.pool.getResource();
            return function.apply(t);
        } finally {
            if (t != null) {
                t.close();
            }
        }
    }

    public final void execute(Consumer<T> consumer) throws Exception {
        T t = null;
        try {
            t = this.pool.getResource();
            consumer.accept(t);
        } finally {
            if (t != null) {
                t.close();
            }
        }
    }
}
