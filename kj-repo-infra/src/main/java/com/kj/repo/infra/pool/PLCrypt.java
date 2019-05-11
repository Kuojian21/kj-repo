package com.kj.repo.infra.pool;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * @param <T>
 * @param <R>
 * @author kuojian21
 */
public abstract class PLCrypt<T, R> {

    private final GenericObjectPool<T> pool;

    public PLCrypt(final GenericObjectPool<T> pool) {
        this.pool = pool;
    }

    public final R execute(BiFunction<T, byte[], R> function, byte[] data) throws Exception {
        T t = null;
        try {
            t = this.pool.borrowObject();
            return function.apply(t, data);
        } finally {
            if (t != null) {
                this.pool.returnObject(t);
            }
        }
    }

    public R crypt(byte[] data) throws Exception {
        return this.execute(this.func(), data);
    }

    public abstract BiFunction<T, byte[], R> func();

    /**
     * http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html
     */
    public enum Transformation {
        AES_CBC_NoPadding_128("AES", "CBC", "NoPadding", 128),
        AES_CBC_PKCS5Padding_128("AES", "CBC", "PKCS5Padding", 128),
        AES_ECB_NoPadding_128("AES", "ECB", "NoPadding", 128),
        AES_ECB_PKCS5Padding_128("AES", "ECB", "PKCS5Padding", 128),
        DES_CBC_NoPadding_56("DES", "CBC", "NoPadding", 56),
        DES_CBC_PKCS5Padding_56("DES", "CBC", "PKCS5Padding", 56),
        DES_ECB_NoPadding_56("DES", "ECB", "NoPadding", 56),
        DES_ECB_PKCS5Padding_56("DES", "ECB", "PKCS5Padding", 56),
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

    /**
     * @author kuojian21
     */
    @FunctionalInterface
    public interface BiFunction<T, U, R> {
        R apply(T t, U u) throws Exception;
    }

    /**
     * @author kuojian21
     */
    public static class KjCipher {

        public static PLCrypt<Cipher, byte[]> encrypt(String algorithm, Key key, IvParameterSpec ivp) {
            return new PLCrypt<Cipher, byte[]>(new GenericObjectPool<Cipher>(new BasePooledObjectFactory<Cipher>() {
                @Override
                public PooledObject<Cipher> wrap(Cipher cipher) {
                    return new DefaultPooledObject<Cipher>(cipher);
                }

                @Override
                public Cipher create() throws Exception {
                    Cipher cipher = Cipher.getInstance(algorithm);
                    cipher.init(Cipher.ENCRYPT_MODE, key, ivp);
                    return cipher;
                }
            })) {
                @Override
                public BiFunction<Cipher, byte[], byte[]> func() {
                    return Helper::cipher;
                }

            };
        }

        public static PLCrypt<Cipher, byte[]> decrypt(String algorithm, Key key, IvParameterSpec ivp) {
            return new PLCrypt<Cipher, byte[]>(new GenericObjectPool<Cipher>(new BasePooledObjectFactory<Cipher>() {
                @Override
                public PooledObject<Cipher> wrap(Cipher cipher) {
                    return new DefaultPooledObject<Cipher>(cipher);
                }

                @Override
                public Cipher create() throws Exception {
                    Cipher cipher = Cipher.getInstance(algorithm);
                    cipher.init(Cipher.DECRYPT_MODE, key, ivp);
                    return cipher;
                }
            })) {
                @Override
                public BiFunction<Cipher, byte[], byte[]> func() {
                    return Helper::cipher;
                }
            };
        }

    }

    /**
     * @author kuojian21
     */
    public static class KjMessageDigest {
        public static PLCrypt<MessageDigest, byte[]> digest(String algorithm) {
            return new PLCrypt<MessageDigest, byte[]>(
                    new GenericObjectPool<MessageDigest>(new BasePooledObjectFactory<MessageDigest>() {
                        @Override
                        public PooledObject<MessageDigest> wrap(MessageDigest digest) {
                            return new DefaultPooledObject<MessageDigest>(digest);
                        }

                        @Override
                        public MessageDigest create() throws Exception {
                            return MessageDigest.getInstance(algorithm);
                        }
                    })) {

                @Override
                public BiFunction<MessageDigest, byte[], byte[]> func() {
                    return Helper::digest;
                }
            };
        }
    }

    /**
     * @author kuojian21
     */
    public static class KjMac {
        public static PLCrypt<Mac, byte[]> mac(String algorithm, SecretKey key) {
            return new PLCrypt<Mac, byte[]>(new GenericObjectPool<Mac>(new BasePooledObjectFactory<Mac>() {
                @Override
                public PooledObject<Mac> wrap(Mac mac) {
                    return new DefaultPooledObject<Mac>(mac);
                }

                @Override
                public Mac create() throws Exception {
                    Mac mac = Mac.getInstance(algorithm);
                    mac.init(key);
                    return mac;
                }
            })) {

                @Override
                public BiFunction<Mac, byte[], byte[]> func() {
                    return Helper::mac;
                }

            };
        }
    }

    /**
     * @author kuojian21
     */
    public static class KjSignature {
        public static PLCrypt<Signature, byte[]> sign(String algorithm, PrivateKey privateKey) {
            return new PLCrypt<Signature, byte[]>(
                    new GenericObjectPool<Signature>(new BasePooledObjectFactory<Signature>() {
                        @Override
                        public PooledObject<Signature> wrap(Signature signature) {
                            return new DefaultPooledObject<Signature>(signature);
                        }

                        @Override
                        public Signature create() throws Exception {
                            Signature signature = Signature.getInstance(algorithm);
                            signature.initSign(privateKey);
                            return signature;
                        }
                    })) {
                @Override
                public BiFunction<Signature, byte[], byte[]> func() {
                    return Helper::sign;
                }
            };
        }

        public static PLCrypt<Signature, Boolean> verify(String algorithm, PublicKey publicKey) {
            return new PLCrypt<Signature, Boolean>(
                    new GenericObjectPool<Signature>(new BasePooledObjectFactory<Signature>() {
                        @Override
                        public PooledObject<Signature> wrap(Signature signature) {
                            return new DefaultPooledObject<Signature>(signature);
                        }

                        @Override
                        public Signature create() throws Exception {
                            Signature signature = Signature.getInstance(algorithm);
                            signature.initVerify(publicKey);
                            return signature;
                        }
                    })) {
                @Override
                public BiFunction<Signature, byte[], Boolean> func() {
                    return Helper::verify;
                }
            };
        }

    }

    /**
     * @author kuojian21
     */
    public static class Factory {

        static {
            Security.addProvider(new BouncyCastleProvider());
        }

        public static SecretKey loadKey(String keyAlgorithm, byte[] key)
                throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException {
            return SecretKeyFactory.getInstance(keyAlgorithm).generateSecret(new DESedeKeySpec(key));
        }

        public static IvParameterSpec loadIvp(byte[] padding) {
            return new IvParameterSpec(padding);
        }

        public static PublicKey loadPublicKey(String algorithm, byte[] key)
                throws InvalidKeySpecException, NoSuchAlgorithmException {
            return KeyFactory.getInstance(algorithm).generatePublic(new X509EncodedKeySpec(key));
        }

        public static PrivateKey loadPrivateKey(String algorithm, byte[] key)
                throws InvalidKeySpecException, NoSuchAlgorithmException {
            return KeyFactory.getInstance(algorithm).generatePrivate(new PKCS8EncodedKeySpec(key));
        }

        public static SecretKey generateKey(String algorithm, Integer keysize, AlgorithmParameterSpec params)
                throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
            KeyGenerator kgen = KeyGenerator.getInstance(algorithm);
            kgen.init(new SecureRandom());
            if (keysize != null) {
                kgen.init(keysize);
            }
            if (params != null) {
                kgen.init(params);
            }
            return kgen.generateKey();
        }

        public static KeyPair generateKeyPair(String algorithm, Integer keysize, AlgorithmParameterSpec params)
                throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
            KeyPairGenerator kgen = KeyPairGenerator.getInstance(algorithm);
            if (keysize != null) {
                kgen.initialize(keysize, new SecureRandom());
            } else if (params != null) {
                kgen.initialize(params, new SecureRandom());
            }
            return kgen.generateKeyPair();
        }

    }

    /**
     * @author kuojian21
     */
    public static class Helper {
        public static byte[] cipher(Cipher cipher, byte[] data) throws IllegalBlockSizeException, BadPaddingException {
            return cipher.doFinal(data);
        }

        public static byte[] digest(MessageDigest digest, byte[] data) {
            return digest.digest(data);
        }

        public static byte[] mac(Mac mac, byte[] data) {
            return mac.doFinal(data);
        }

        public static byte[] sign(Signature signature, byte[] data) throws SignatureException {
            signature.update(data);
            return signature.sign();

        }

        public static boolean verify(Signature signature, byte[] data) throws SignatureException {
            return signature.verify(data);
        }
    }

    /**
     * @author kuojian21
     */
    public static class Algorithm {
        public enum Mac {
            HmacMD5, HmacSHA1, HmacSHA256;

            public String getName() {
                return this.name();
            }
        }

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

        public enum Signature {
            SHA1withDSA, SHA1withRSA, SHA256withRSA;

            public String getName() {
                return this.name();
            }
        }

        public enum Crypt {
            DiffieHellman("DiffieHellman", 1024), DSA("DSA", 1024), RSA_1024("RSA", 1024), RSA_2048("RSA", 2048), AES(
                    "AES ", 128),
            DES("DES", 56), DESede("DESede", 168);

            private final String name;
            private final int keysize;
            private Crypt(String name, int keysize) {
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

    }

}
