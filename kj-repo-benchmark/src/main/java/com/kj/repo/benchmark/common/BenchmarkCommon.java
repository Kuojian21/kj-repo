package com.kj.repo.benchmark.common;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
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

/**
 * @author kj
 * Created on 2020-03-26
 */
@BenchmarkMode({Mode.AverageTime})
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class BenchmarkCommon {
    private int a = 10;
    private int b = 10;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BenchmarkCommon.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .warmupBatchSize(1)
                .warmupTime(TimeValue.seconds(1))
                .warmupMode(WarmupMode.INDI)
                .measurementIterations(3)
                .measurementBatchSize(1)
                .measurementTime(TimeValue.seconds(10))
                .threads(1)
                .timeout(TimeValue.seconds(3))
                .syncIterations(true)
                //                .addProfiler(DTraceSystemCallProfiler.class)
                .build();
        new Runner(opt).run();
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private int sink(int s) {
        return s;
    }

    @Benchmark
    public void add(Blackhole bh) {
        bh.consume(a + b);
    }

    @Benchmark
    public int addSink() {
        return sink(a + b);
    }
    //
    //
    //    @Benchmark
    //    public double baseline() {
    //        return Math.PI;
    //    }
    //
    //    @Benchmark
    //    public long millis() {
    //        return System.currentTimeMillis();
    //    }
    //
    //
    //    private Random random = new Random();
    //
    //    @Benchmark
    //    public int random() {
    //        return random.nextInt();
    //    }
    //
    //    @Benchmark
    //    public int threadLocalRandom() {
    //        return ThreadLocalRandom.current().nextInt();
    //    }
    //
    //    @Benchmark
    //    public synchronized void sync(Blackhole bh) {
    //        bh.consume(a + b);
    //    }


    @Benchmark
    public void throwable(Blackhole bh) {
        bh.consume(new Throwable());
    }

}
