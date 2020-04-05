package com.kj.repo.infra.crypt;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import org.apache.commons.pool2.impl.GenericObjectPool;

import com.kj.repo.infra.base.function.Consumer;
import com.kj.repo.infra.base.function.Function;
import com.kj.repo.infra.base.pool.Pool;
import com.kj.repo.infra.helper.GenericPoolHelper;

/**
 * @author kj
 * Created on 2020-03-14
 */
public abstract class CryptSign extends Pool<Signature> {
    public CryptSign(GenericObjectPool<Signature> pool) {
        super(pool);
    }

    public static Sign sign(String algorithm, PrivateKey privateKey) {
        return new Sign(GenericPoolHelper.wrap(() -> {
            Signature signature = Signature.getInstance(algorithm);
            signature.initSign(privateKey);
            return signature;
        }, sig -> {}));
    }

    public static Verify verify(String algorithm, PublicKey publicKey) {
        return new Verify(GenericPoolHelper.wrap(() -> {
            Signature signature = Signature.getInstance(algorithm);
            signature.initVerify(publicKey);
            return signature;
        }, sig -> {}));
    }

    public byte[] doExecute(Function<Signature, byte[]> function) throws Exception {
        return super.execute(function);
    }

    public void doExecute(Consumer<Signature> consumer) throws Exception {
        super.execute(consumer);
    }

    /**
     * @author kj
     */
    public static class Sign extends CryptSign {
        public Sign(GenericObjectPool<Signature> pool) {
            super(pool);
        }

        public final byte[] sign(byte[] data) throws Exception {
            return super.execute(sig -> {
                sig.update(data);
                return sig.sign();
            });
        }
    }

    /**
     * @author kj
     */
    public static class Verify extends CryptSign {
        public Verify(GenericObjectPool<Signature> pool) {
            super(pool);
        }

        public final boolean verify(byte[] data, byte[] sign) throws Exception {
            return super.execute(sig -> {
                sig.update(data);
                return sig.verify(sign);
            });
        }
    }
}
