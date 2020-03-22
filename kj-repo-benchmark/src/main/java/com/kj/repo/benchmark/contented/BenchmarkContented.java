package com.kj.repo.benchmark.contented;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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
import org.openjdk.jmh.runner.options.WarmupMode;
import org.slf4j.Logger;

import com.kj.repo.infra.logger.LoggerHelper;

/**
 * @author kj
 * Created on 2020-03-21
 */
@BenchmarkMode({Mode.Throughput})
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BenchmarkContented {
    private static final Logger logger = LoggerHelper.getLogger();
    private final ContentedY y = new ContentedY();
    private final ContentedN n = new ContentedN();
    @Param({"10000", "100000", "1000000"})
    private int count;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BenchmarkContented.class.getSimpleName())
                .forks(1)
                .warmupIterations(0)
                .warmupBatchSize(1)
                .warmupTime(TimeValue.seconds(0))
                .warmupMode(WarmupMode.INDI)
                .measurementIterations(3)
                .measurementBatchSize(1)
                .measurementTime(TimeValue.seconds(10))
                .threads(1)
                .timeout(TimeValue.seconds(3))
                .syncIterations(true)
                .addProfiler(JVMOptionRestrictContended.class)
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public void runY() throws InterruptedException {
        Thread threadA = new Thread(() -> IntStream.range(0, count).forEach(i -> y.a++), "runY-A");
        Thread threadB = new Thread(() -> IntStream.range(0, count).forEach(i -> y.b++), "runY-B");
        Thread threadC = new Thread(() -> IntStream.range(0, count).forEach(i -> y.c++), "runY-C");
        Thread threadD = new Thread(() -> IntStream.range(0, count).forEach(i -> y.d++), "runY-D");
        Thread threadE = new Thread(() -> IntStream.range(0, count).forEach(i -> y.e++), "runY-E");
        Thread threadF = new Thread(() -> IntStream.range(0, count).forEach(i -> y.f++), "runY-F");
        threadA.start();
        threadB.start();
        threadC.start();
        threadD.start();
        threadE.start();
        threadF.start();
        threadA.join();
        threadB.join();
        threadC.join();
        threadD.join();
        threadE.join();
        threadF.join();
    }

    @Benchmark
    public void runN() throws InterruptedException {
        Thread threadA = new Thread(() -> IntStream.range(0, count).forEach(i -> n.a++), "runN-A");
        Thread threadB = new Thread(() -> IntStream.range(0, count).forEach(i -> n.b++), "runN-B");
        Thread threadC = new Thread(() -> IntStream.range(0, count).forEach(i -> n.c++), "runN-C");
        Thread threadD = new Thread(() -> IntStream.range(0, count).forEach(i -> n.d++), "runN-D");
        Thread threadE = new Thread(() -> IntStream.range(0, count).forEach(i -> n.e++), "runN-E");
        Thread threadF = new Thread(() -> IntStream.range(0, count).forEach(i -> n.f++), "runN-F");
        threadA.start();
        threadB.start();
        threadC.start();
        threadD.start();
        threadE.start();
        threadF.start();
        threadA.join();
        threadB.join();
        threadC.join();
        threadD.join();
        threadE.join();
        threadF.join();
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
