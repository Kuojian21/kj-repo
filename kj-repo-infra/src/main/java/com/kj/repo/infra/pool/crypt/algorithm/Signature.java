package com.kj.repo.infra.pool.crypt.algorithm;

public enum Signature {
    SHA1withDSA, SHA1withRSA, SHA256withRSA;

    public String getName() {
        return this.name();
    }
}