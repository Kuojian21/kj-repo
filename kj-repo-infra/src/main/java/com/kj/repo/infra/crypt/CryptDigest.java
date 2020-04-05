package com.kj.repo.infra.crypt;

import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.apache.commons.pool2.impl.GenericObjectPool;

import com.kj.repo.infra.base.function.Consumer;
import com.kj.repo.infra.base.function.Function;
import com.kj.repo.infra.base.pool.Pool;
import com.kj.repo.infra.helper.GenericPoolHelper;

/**
 * @author kj
 * Created on 2020-03-14
 */
public abstract class CryptDigest<T> extends Pool<T> {

    private final Consumer<T> reset;

    public CryptDigest(GenericObjectPool<T> pool, Consumer<T> reset) {
        super(pool);
        this.reset = reset;
    }

    public static CryptDigest<Mac> mac(String algorithm, SecretKey key) {
        return new CryptDigest<Mac>(GenericPoolHelper.wrap(() -> {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(key);
            return mac;
        }, Mac::reset), Mac::reset) {
            @Override
            public byte[] digest(byte[] src) throws Exception {
                return super.digest(mac -> {
                    mac.update(src);
                    return mac.doFinal();
                });
            }
        };
    }

    public static CryptDigest<MessageDigest> digest(String algorithm) {
        return new CryptDigest<MessageDigest>(
                GenericPoolHelper.wrap(() -> MessageDigest.getInstance(algorithm), MessageDigest::reset),
                MessageDigest::reset) {
            @Override
            public byte[] digest(byte[] src) throws Exception {
                return super.digest(digest -> {
                    digest.update(src);
                    return digest.digest();
                });
            }
        };
    }


    public abstract byte[] digest(byte[] src) throws Exception;

    public <R> R digest(Function<T, R> function) throws Exception {
        return super.execute(t -> {
            R rtn = function.apply(t);
            reset.accept(t);
            return rtn;
        });
    }

    public final void digest(Consumer<T> consumer) throws Exception {
        super.execute(t -> {
            consumer.accept(t);
            reset.accept(t);
        });
    }

}
