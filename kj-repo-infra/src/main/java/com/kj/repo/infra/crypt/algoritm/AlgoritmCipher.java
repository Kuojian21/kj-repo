package com.kj.repo.infra.crypt.algoritm;

public enum AlgoritmCipher {
    DiffieHellman("DiffieHellman", 1024),
    DSA("DSA", 1024),
    RSA_1024("RSA", 1024),
    RSA_2048("RSA", 2048),
    AES("AES ", 128),
    DES("DES", 56),
    DESede("DESede", 168);

    private final String name;
    private final int keysize;

    AlgoritmCipher(String name, int keysize) {
        this.name = name;
        this.keysize = keysize;
    }

    public String getName() {
        return name;
    }

    public int getKeysize() {
        return keysize;
    }
}
