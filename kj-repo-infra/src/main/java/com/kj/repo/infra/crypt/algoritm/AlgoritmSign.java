package com.kj.repo.infra.crypt.algoritm;

/**
 * @author kj
 * Created on 2020-03-14
 */
public enum AlgoritmSign {
    SHA1withDSA,
    SHA1withRSA,
    SHA256withRSA;

    public String getName() {
        return this.name();
    }
}
