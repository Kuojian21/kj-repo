package com.kj.repo.benchmark.crypt;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
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
import org.openjdk.jmh.runner.options.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kj.repo.infra.base.function.Function;
import com.kj.repo.infra.crypt.CryptCipher;
import com.kj.repo.infra.crypt.algoritm.AlgoritmCipher;
import com.kj.repo.infra.crypt.key.CryptKey;
import com.kj.repo.infra.helper.RunHelper;

/**
 * @author kj
 * Created on 2020-03-14
 */
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class CryptBenchmark {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Key key;
    private final IvParameterSpec ivp;
    private final CryptCipher cipher;
    @Param({"1", "3", "9"})
    private int count;

    public CryptBenchmark() {
        try {
            key = CryptKey.generateKey(AlgoritmCipher.DESede.getName(), AlgoritmCipher.DESede.getKeysize());
            ivp = CryptKey.loadIvp("kuojian".getBytes());
            cipher = new CryptCipher(AlgoritmCipher.DESede.getName(), key, ivp);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CryptBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .warmupBatchSize(1)
                .warmupTime(TimeValue.minutes(1))
                .measurementIterations(10)
                .measurementBatchSize(1)
                .measurementTime(TimeValue.minutes(10))
                .threads(Runtime.getRuntime().availableProcessors())
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public void crypt1() {
        run(j -> {
                    Cipher encrypt = Cipher.getInstance(AlgoritmCipher.DESede.getName());
                    encrypt.init(Cipher.ENCRYPT_MODE, key, ivp);

                    Cipher decrypt = Cipher.getInstance(AlgoritmCipher.DESede.getName());
                    decrypt.init(Cipher.DECRYPT_MODE, key, ivp);
                    return new String(decrypt.doFinal(encrypt.doFinal(("kuojian_" + j).getBytes())));
                }
        );
    }

    @Benchmark
    public void crypt2() {
        run((j) -> new String(cipher.decrypt(cipher.encrypt(("kuojian_" + j).getBytes()))));
    }

    public void run(Function<Integer, String> function) {
        IntStream.range(0, count).boxed().forEach(j ->
                logger.info("j:{} rtn:{}", j, RunHelper.run(() -> function.apply(j))));
    }

    @Setup
    public void setup() {
        logger.info("setup");
    }

    @TearDown
    public void down() {
        logger.info("down");
    }
}
