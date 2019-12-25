package com.kj.repo.infra.pool.crypt;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.kj.repo.infra.pool.PLBase;

/**
 * @author kj
 */
public class PLSignature extends PLBase<Signature> {
    public PLSignature(GenericObjectPool<Signature> pool) {
        super(pool);
    }

    public static PLSignature sign(String algorithm, PrivateKey privateKey) {
        return new PLSignature(new GenericObjectPool<Signature>(new BasePooledObjectFactory<Signature>() {
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
        }));
    }

    public static PLSignature verify(String algorithm, PublicKey publicKey) {
        return new PLSignature(new GenericObjectPool<Signature>(new BasePooledObjectFactory<Signature>() {
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
        }));
    }

    public byte[] sign(byte[] data) throws Exception {
        return this.execute(signature -> {
            signature.update(data);
            return signature.sign();
        });

    }

    public boolean verify(byte[] data) throws Exception {
        return this.execute(signature -> {
            return signature.verify(data);
        });
    }

}