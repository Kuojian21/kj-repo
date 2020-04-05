package com.kj.repo.benchmark.jit;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.CompilerProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.WarmupMode;

/**
 * @author kj
 * Created on 2020-03-25
 */
@BenchmarkMode({Mode.AverageTime})
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.SECONDS)
@Threads(1)
public class BenchmarkJIT {
    private static final double EARTH_RADIUS = 6378.137 * 1000;
    private static final double ALL_DEGREES = 180.0;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BenchmarkJIT.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .warmupBatchSize(1)
                .warmupTime(TimeValue.seconds(1))
                .warmupMode(WarmupMode.INDI)
                .measurementIterations(1)
                .measurementBatchSize(1)
                .measurementTime(TimeValue.seconds(300))
                .threads(1)
                .timeout(TimeValue.seconds(3))
                .syncIterations(true)
                .addProfiler(CompilerProfiler.class)
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public void jit() {
        distance(40.2143, 50.43243, 56.43214, 54.535);
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public double distance(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double radlng1 = rad(lng1);
        double radlng2 = rad(lng2);
        double a = radLat1 - radLat2;
        double b = radlng1 - radlng2;
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        return s;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    private double rad(double d) {
        return d * Math.PI / ALL_DEGREES;
    }
}