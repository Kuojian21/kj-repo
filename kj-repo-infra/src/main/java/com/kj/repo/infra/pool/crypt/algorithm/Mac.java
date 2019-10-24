package com.kj.repo.infra.pool.crypt.algorithm;

public enum Mac {
    HmacMD5, HmacSHA1, HmacSHA256;

    public String getName() {
        return this.name();
    }
}