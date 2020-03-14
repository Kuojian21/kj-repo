package com.kj.repo.infra.crypt;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.kj.repo.infra.base.BasePool;
import com.kj.repo.infra.base.function.Consumer;
import com.kj.repo.infra.base.function.Function;

/**
 * @author kj
 * Created on 2020-03-14
 */
public class CryptCipher {
    private final BasePool<Cipher> encrypt;
    private final BasePool<Cipher> decrypt;

    public CryptCipher(String algorithm, Key key, IvParameterSpec ivp) {
        encrypt = new BasePool<>(new GenericObjectPool<>(new BasePooledObjectFactory<Cipher>() {
            @Override
            public PooledObject<Cipher> wrap(Cipher cipher) {
                return new DefaultPooledObject<>(cipher);
            }

            @Override
            public Cipher create() throws Exception {
                Cipher cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.ENCRYPT_MODE, key, ivp);
                return cipher;
            }
        }));
        decrypt = new BasePool<>(new GenericObjectPool<>(new BasePooledObjectFactory<Cipher>() {
            @Override
            public PooledObject<Cipher> wrap(Cipher cipher) {
                return new DefaultPooledObject<>(cipher);
            }

            @Override
            public Cipher create() throws Exception {
                Cipher cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.DECRYPT_MODE, key, ivp);
                return cipher;
            }
        }));
    }

    public static CryptCipher crypt(String algorithm, Key key, IvParameterSpec ivp) {
        return new CryptCipher(algorithm, key, ivp);
    }

    public byte[] encrypt(byte[] src) throws Exception {
        return this.encrypt.execute(cipher -> {
            cipher.update(src);
            return cipher.doFinal();
        });
    }

    public byte[] decrypt(byte[] src) throws Exception {
        return this.decrypt.execute(cipher -> {
            cipher.update(src);
            return cipher.doFinal();
        });
    }

    public <R> R encrypt(Function<Cipher, R> function) throws Exception {
        return this.encrypt.execute(function);
    }

    public void encrypt(Consumer<Cipher> consumer) throws Exception {
        this.encrypt.execute(consumer);
    }

    public <R> R decrypt(Function<Cipher, R> function) throws Exception {
        return this.decrypt.execute(function);
    }

    public void decrypt(Consumer<Cipher> consumer) throws Exception {
        this.decrypt.execute(consumer);
    }
}
