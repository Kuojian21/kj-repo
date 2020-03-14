package com.kj.repo.infra.crypt.algoritm;

/**
 * @author kj
 * Created on 2020-03-14
 */
public class AlgoritmDigest {
    /**
     * @author kj
     */
    public enum Digest {
        MD5("MD5"),
        SHA_1("SHA-1"),
        SHA_256("SHA-256");
        private final String name;

        Digest(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * @author kj
     */
    public enum Mac {
        HmacMD5,
        HmacSHA1,
        HmacSHA256;

        public String getName() {
            return this.name();
        }
    }

}
