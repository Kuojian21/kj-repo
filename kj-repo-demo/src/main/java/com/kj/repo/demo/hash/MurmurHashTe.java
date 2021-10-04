package com.kj.repo.demo.hash;

import redis.clients.util.MurmurHash;

/**
 * @author kj
 * Created on 2020-07-30
 */
public class MurmurHashTe {
    public static void main(String[] args) {
        System.out.println(new MurmurHash().hash(""));
    }
}
