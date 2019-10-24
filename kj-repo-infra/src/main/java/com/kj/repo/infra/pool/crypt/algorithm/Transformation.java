package com.kj.repo.infra.pool.crypt.algorithm;

/**
 * http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html
 */
public enum Transformation {
    AES_CBC_NoPadding_128("AES", "CBC", "NoPadding", 128), AES_CBC_PKCS5Padding_128("AES", "CBC", "PKCS5Padding", 128),
    AES_ECB_NoPadding_128("AES", "ECB", "NoPadding", 128), AES_ECB_PKCS5Padding_128("AES", "ECB", "PKCS5Padding", 128),
    DES_CBC_NoPadding_56("DES", "CBC", "NoPadding", 56), DES_CBC_PKCS5Padding_56("DES", "CBC", "PKCS5Padding", 56),
    DES_ECB_NoPadding_56("DES", "ECB", "NoPadding", 56), DES_ECB_PKCS5Padding_56("DES", "ECB", "PKCS5Padding", 56),
    DESede_CBC_NoPadding_168("DESede", "CBC", "NoPadding", 168),
    DESede_CBC_PKCS5Padding_168("DESede", "CBC", "PKCS5Padding", 168),
    DESede_ECB_NoPadding_168("DESede", "ECB", "NoPadding", 168),
    DESede_ECB_PKCS5Padding_168("DESede", "ECB", "PKCS5Padding", 168),
    RSA_ECB_PKCS1Padding_1024("RSA", "ECB", "PKCS1Padding", 1024),
    RSA_ECB_OAEPWithSHA_1AndMGF1Padding_1024("RSA", "ECB", "OAEPWithSHA-1AndMGF1Padding", 1024),
    RSA_ECB_OAEPWithSHA_256AndMGF1Padding_1024("RSA", "ECB", "OAEPWithSHA-256AndMGF1Padding", 1024),
    RSA_ECB_PKCS1Padding_2048("RSA", "ECB", "PKCS1Padding", 2048),
    RSA_ECB_OAEPWithSHA_1AndMGF1Padding_2048("RSA", "ECB", "OAEPWithSHA-1AndMGF1Padding", 2048),
    RSA_ECB_OAEPWithSHA_256AndMGF1Padding_2048("RSA", "ECB", "OAEPWithSHA-256AndMGF1Padding", 2048);

    private final String algorithm;
    private final String mode;
    private final String padding;
    private final int keysize;
    private final String transformation;

    private Transformation(String algorithm, String mode, String padding, int keysize) {
        this.algorithm = algorithm;
        this.mode = mode;
        this.padding = padding;
        this.keysize = keysize;
        this.transformation = this.algorithm + "/" + this.mode + "/" + this.padding;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getMode() {
        return mode;
    }

    public String getPadding() {
        return padding;
    }

    public int getKeysize() {
        return keysize;
    }

    public String getTransformation() {
        return this.transformation;
    }
}