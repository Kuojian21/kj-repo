package com.kj.repo.tt.pool;

import com.google.common.collect.Sets;
import com.kj.repo.infra.pool.jedis.PLJedis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

public class TePLJedis {
    public static void main(String[] args) throws Exception {
        Jedis jedis = PLJedis.jedis(new JedisSentinelPool("master", Sets.newHashSet("ip:port", "ip:port"), "password"),
                Jedis.class);
        jedis.get("");
        System.exit(0);
    }
}
