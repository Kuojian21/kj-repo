package com.kj.repo.infra.pool.crypt;

import java.security.MessageDigest;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.kj.repo.infra.pool.PLBase;

/**
 * @author kuojian21
 */
public class PLMessageDigest extends PLBase<MessageDigest> {

    public PLMessageDigest(GenericObjectPool<MessageDigest> pool) {
        super(pool);
    }

    public static PLMessageDigest messageDigest(String algorithm) {
        return new PLMessageDigest(new GenericObjectPool<MessageDigest>(new BasePooledObjectFactory<MessageDigest>() {
            @Override
            public PooledObject<MessageDigest> wrap(MessageDigest digest) {
                return new DefaultPooledObject<MessageDigest>(digest);
            }

            @Override
            public MessageDigest create() throws Exception {
                return MessageDigest.getInstance(algorithm);
            }
        }));
    }

    public byte[] digest(byte[] data) throws Exception {
        return this.execute(digest -> {
            return digest.digest(data);
        });
    }
}
