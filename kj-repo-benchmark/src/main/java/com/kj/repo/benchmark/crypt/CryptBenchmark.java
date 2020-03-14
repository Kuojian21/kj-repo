package com.kj.repo.benchmark.crypt;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.google.common.collect.Lists;
import com.kj.repo.infra.base.function.BiFunction;
import com.kj.repo.infra.crypt.CryptCipher;
import com.kj.repo.infra.crypt.algoritm.AlgoritmCipher;
import com.kj.repo.infra.crypt.key.CryptKey;
import com.kj.repo.infra.helper.RunHelper;

/**
 * @author kj
 * Created on 2020-03-14
 */
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class CryptBenchmark {

    private final Key key;
    private final IvParameterSpec ivp;
    private final ExecutorService service1;
    private final ExecutorService service2;
    @Param({"10", "100", "1000"})
    private int count;

    public CryptBenchmark() {
        try {
            key = CryptKey.generateKey(AlgoritmCipher.DESede.getName(), AlgoritmCipher.DESede.getKeysize());
            ivp = CryptKey.loadIvp("kuojian".getBytes());
            service1 = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            service2 = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CryptBenchmark.class.getSimpleName())
                .forks(2)
                .warmupIterations(5)
                .measurementIterations(5)
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void crypt1() throws ExecutionException, InterruptedException {
        run(service1,
                (i, j) -> {
                    Cipher encrypt = Cipher.getInstance(AlgoritmCipher.DESede.getName());
                    encrypt.init(Cipher.ENCRYPT_MODE, key, ivp);

                    Cipher decrypt = Cipher.getInstance(AlgoritmCipher.DESede.getName());
                    decrypt.init(Cipher.DECRYPT_MODE, key, ivp);
                    return new String(decrypt.doFinal(encrypt.doFinal(("kuojian_" + i + "_" + j).getBytes())));
                });
    }

    @Benchmark
    public void crypt2() throws ExecutionException, InterruptedException {
        CryptCipher cipher =
                new CryptCipher(AlgoritmCipher.DESede.getName(), key, ivp);
        run(service2, (i, j) -> new String(cipher.decrypt(cipher.encrypt(("kuojian_" + i + "_" + j).getBytes()))));
    }

    public void run(ExecutorService service, BiFunction<Integer, Integer, String> function)
            throws ExecutionException, InterruptedException {
        List<Future<?>> futures = Lists.newArrayList();
        IntStream.range(0, count).boxed().forEach(i ->
                futures.add(service.submit(() -> {
                    IntStream.range(0, 10).boxed().forEach(j -> {
                        System.out.println(RunHelper.run(() -> function.apply(i, j)));
                    });
                }))
        );
        for (Future<?> future : futures) {
            future.get();
        }
    }

    @Setup
    public void setup() {
        System.out.println("setup");
    }

    @TearDown
    public void down() {
        service1.shutdown();
        service2.shutdown();
        System.out.println("down");
    }
}
