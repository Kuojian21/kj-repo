package com.kj.repo.infra.pool.crypt;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.kj.repo.infra.pool.PLBase;

/**
 * @author kj
 */
public class PLMac extends PLBase<Mac> {
    public PLMac(GenericObjectPool<Mac> pool) {
        super(pool);
    }

    public static PLMac mac(String algorithm, SecretKey key) {
        return new PLMac(new GenericObjectPool<Mac>(new BasePooledObjectFactory<Mac>() {
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
        }));
    }

    public byte[] mac(byte[] data) throws Exception {
        return this.execute(mac -> {
            return mac.doFinal(data);
        });
    }
}