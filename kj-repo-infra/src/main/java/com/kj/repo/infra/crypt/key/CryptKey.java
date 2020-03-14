package com.kj.repo.infra.crypt.key;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * @author kj
 */
public class CryptKey {

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

    public static SecretKey generateKey(String algorithm)
            throws NoSuchAlgorithmException {
        KeyGenerator kgen = KeyGenerator.getInstance(algorithm);
        kgen.init(new SecureRandom());
        return kgen.generateKey();
    }

    public static SecretKey generateKey(String algorithm, int keysize)
            throws NoSuchAlgorithmException {
        KeyGenerator kgen = KeyGenerator.getInstance(algorithm);
        kgen.init(new SecureRandom());
        kgen.init(keysize);
        return kgen.generateKey();
    }

    public static SecretKey generateKey(String algorithm, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyGenerator kgen = KeyGenerator.getInstance(algorithm);
        kgen.init(new SecureRandom());
        kgen.init(params);
        return kgen.generateKey();
    }


    public static SecretKey generateKey(String algorithm, int keysize, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyGenerator kgen = KeyGenerator.getInstance(algorithm);
        kgen.init(new SecureRandom());
        kgen.init(keysize);
        kgen.init(params);
        return kgen.generateKey();
    }

    public static KeyPair generateKeyPair(String algorithm, Integer keysize)
            throws NoSuchAlgorithmException {
        KeyPairGenerator kgen = KeyPairGenerator.getInstance(algorithm);
        kgen.initialize(keysize, new SecureRandom());
        return kgen.generateKeyPair();
    }

    public static KeyPair generateKeyPair(String algorithm, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator kgen = KeyPairGenerator.getInstance(algorithm);
        kgen.initialize(params, new SecureRandom());
        return kgen.generateKeyPair();
    }

}