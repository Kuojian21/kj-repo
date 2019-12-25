package com.kj.repo.infra.pool.crypt;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.kj.repo.infra.pool.PLBase;

/**
 * @author kj
 */
public class PLCipher extends PLBase<Cipher> {

    public PLCipher(GenericObjectPool<Cipher> pool) {
        super(pool);
    }

    public static PLCipher encrypt(String algorithm, Key key, IvParameterSpec ivp) {
        return crypt(algorithm, key, ivp, Cipher.ENCRYPT_MODE);
    }

    public static PLCipher decrypt(String algorithm, Key key, IvParameterSpec ivp) {
        return crypt(algorithm, key, ivp, Cipher.DECRYPT_MODE);
    }

    public static PLCipher crypt(String algorithm, Key key, IvParameterSpec ivp, int opmode) {
        return new PLCipher(new GenericObjectPool<Cipher>(new BasePooledObjectFactory<Cipher>() {
            @Override
            public PooledObject<Cipher> wrap(Cipher cipher) {
                return new DefaultPooledObject<Cipher>(cipher);
            }

            @Override
            public Cipher create() throws Exception {
                Cipher cipher = Cipher.getInstance(algorithm);
                cipher.init(opmode, key, ivp);
                return cipher;
            }
        }));
    }

    public byte[] cipher(byte[] data) throws Exception {
        return this.execute(cipher -> {
            return cipher.doFinal(data);
        });
    }

}