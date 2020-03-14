package com.kj.repo.tt.jedis;

import com.google.common.collect.Sets;
import com.kj.repo.infra.jedis.JedisTL;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

/**
 * @author kj
 */
public class TeJedisTL {
    public static void main(String[] args) throws Exception {
        JedisSentinelPool pool = new JedisSentinelPool("master", Sets.newHashSet("localhost:26379"), "password");
        Jedis jedis = JedisTL.jedisTL(pool).jedis();
        jedis.get("");
        System.exit(0);
    }
}
