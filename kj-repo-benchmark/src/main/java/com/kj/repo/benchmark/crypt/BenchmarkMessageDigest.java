package com.kj.repo.benchmark.crypt;

import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.WarmupMode;

import com.kj.repo.infra.crypt.CryptDigest;
import com.kj.repo.infra.crypt.algoritm.AlgoritmDigest.Digest;

/**
 * @author kj
 * Created on 2020-04-20
 */
@BenchmarkMode({Mode.AverageTime})
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class BenchmarkMessageDigest {

    private CryptDigest digest = CryptDigest.digest(Digest.MD5.getName());

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BenchmarkMessageDigest.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .warmupBatchSize(1)
                .warmupTime(TimeValue.seconds(30))
                .warmupMode(WarmupMode.INDI)
                .measurementIterations(3)
                .measurementBatchSize(1)
                .measurementTime(TimeValue.seconds(10))
                .threads(Runtime.getRuntime().availableProcessors() / 2 + 1)
                .timeout(TimeValue.minutes(3))
                .syncIterations(true)
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public void digest1(Blackhole bh) throws Exception {
        bh.consume(digest.digest("com.kj.repo.benchmark.crypt.BenchmarkMessageDigest".getBytes()));
    }

    @Benchmark
    public void digest2(Blackhole bh) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(Digest.MD5.getName());
        bh.consume(digest.digest("com.kj.repo.benchmark.crypt.BenchmarkMessageDigest".getBytes()));
    }

}
