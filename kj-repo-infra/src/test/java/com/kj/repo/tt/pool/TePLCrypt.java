package com.kj.repo.tt.pool;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

import com.google.common.base.Stopwatch;
import com.kj.repo.infra.pool.crypt.PLCipher;
import com.kj.repo.infra.pool.crypt.algorithm.Crypt;
import com.kj.repo.infra.pool.crypt.factory.CryptFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TePLCrypt {

    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, InvalidKeySpecException, InterruptedException, ExecutionException {

        Key key = CryptFactory.generateKey(Crypt.DESede.getName(), Crypt.DESede.getKeysize(), null);
        System.out.println(Base64.getEncoder().encodeToString(key.getEncoded()));
        KeyPair keyPair = CryptFactory.generateKeyPair(Crypt.RSA_2048.getName(), Crypt.RSA_2048.getKeysize(), null);
        System.out.println(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        System.out.println(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));

        perf("DESede/CBC/PKCS5Padding",
                CryptFactory.loadKey("DESede", Base64.getDecoder().decode("L/gx33BYzjQLj12FhlSXlEosuVSwKl1M")),
                CryptFactory.loadIvp("11111111".getBytes()), 10000000, 10, 1);

    }

    public static void perf(String algorithm, Key key, IvParameterSpec ivp, int total, int thread, int times)
            throws InterruptedException, InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException,
            ExecutionException {
        long p1 = perf1(algorithm, key, ivp, total, thread, times);
        long p2 = perf2(algorithm, key, ivp, total, thread, times);
        log.info("{} {}", p1, p2);
    }

    public static long perf1(String algorithm, Key key, IvParameterSpec ivp, int total, int thread, int times)
            throws InterruptedException, InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException,
            ExecutionException {
        PLCipher encrypt = PLCipher.encrypt(algorithm, key, ivp);
        PLCipher decrypt = PLCipher.decrypt(algorithm, key, ivp);
        ExecutorService executor = Executors.newFixedThreadPool(thread);

        Stopwatch stopwatch = Stopwatch.createStarted();
        Queue<Future<Long>> futures = new ConcurrentLinkedQueue<Future<Long>>();

        new Thread(() -> {
            IntStream.range(0, total).boxed().forEach(j -> {
                futures.add(executor.submit(() -> {
                    try {
                        long rtn = 0;
                        for (int i = 0; i < times; i++) {
                            rtn += new String(decrypt.cipher(encrypt.cipher(("kuojian" + j).getBytes()))).hashCode()
                                    % Integer.MAX_VALUE;
                        }
                        return rtn;
                    } catch (Exception e) {
                        log.error("", e);
                        return 0L;
                    }
                }));
            });
        }).start();

        long rtn = 0;
        for (int i = 0; i < total; i++) {
            Future<Long> future = futures.poll();
            if (future == null) {
                i--;
                Thread.sleep(1000);
            } else {
                rtn += future.get() % Integer.MAX_VALUE;
            }
        }
        log.info("{}", rtn);
        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.MINUTES);
        return stopwatch.elapsed(TimeUnit.MILLISECONDS);
    }

    public static long perf2(String algorithm, Key key, IvParameterSpec ivp, int total, int thread, int times)
            throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(thread);

        Stopwatch stopwatch = Stopwatch.createStarted();

        Queue<Future<Long>> futures = new ConcurrentLinkedQueue<Future<Long>>();

        new Thread(() -> {
            IntStream.range(0, total).boxed().forEach(j -> {
                futures.add(executor.submit(() -> {
                    try {
                        Cipher encrypt = Cipher.getInstance(algorithm);
                        encrypt.init(Cipher.ENCRYPT_MODE, key, ivp);

                        Cipher decrypt = Cipher.getInstance(algorithm);
                        decrypt.init(Cipher.DECRYPT_MODE, key, ivp);

                        long rtn = 0;
                        for (int i = 0; i < times; i++) {
                            rtn += new String(decrypt.doFinal(encrypt.doFinal(("kuojian" + j).getBytes()))).hashCode()
                                    % Integer.MAX_VALUE;
                        }
                        return rtn;
                    } catch (Exception e) {
                        log.error("", e);
                        return 0L;
                    }
                }));
            });
        }).start();
        long rtn = 0;
        for (int i = 0; i < total; i++) {
            Future<Long> future = futures.poll();
            if (future == null) {
                i--;
                Thread.sleep(1000);
            } else {
                rtn += future.get() % Integer.MAX_VALUE;
            }
        }
        log.info("{}", rtn);
        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.MINUTES);
        return stopwatch.elapsed(TimeUnit.MILLISECONDS);
    }

}
