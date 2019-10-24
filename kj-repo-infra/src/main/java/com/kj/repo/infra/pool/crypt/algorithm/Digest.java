package com.kj.repo.infra.pool.crypt.algorithm;

public enum Digest {
    MD5("MD5"), SHA_1("SHA-1"), SHA_256("SHA-256");
    private final String name;

    private Digest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}