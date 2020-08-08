package com.kj.repo.benchmark.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
 * Created on 2020-08-03
 */
@BenchmarkMode({Mode.AverageTime})
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.SECONDS)
@Threads(1)
public class BenchmarkReflect {
    public static void main(String[] args) throws RunnerException, NoSuchMethodException {
        Options opt = new OptionsBuilder()
                .include(BenchmarkReflect.class.getSimpleName())
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

    private final Bean bean;
    private final Method method1;
    private final Method method2;

    public BenchmarkReflect() {
        try {
            bean = new Bean();
            method1 = Bean.class.getMethod("bean1");
            method2 = Bean.class.getMethod("bean2");
            method2.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    @Benchmark
    public void run1() {
        bean.bean1();
    }

    @Benchmark
    public void run2() {
        bean.bean1();
    }

    @Benchmark
    public void run3() throws InvocationTargetException, IllegalAccessException {
        method1.invoke(bean);
    }

    @Benchmark
    public void run4() throws InvocationTargetException, IllegalAccessException {
        method2.invoke(bean);
    }

}

class Bean {
    public void bean1() {

    }

    public void bean2() {

    }
}
