package com.kj.repo.benchmark.hostname;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author kj
 * Created on 2020-03-18
 */
@BenchmarkMode({Mode.SingleShotTime})
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BenchmarkHostname {
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BenchmarkHostname.class.getSimpleName())
                .forks(3)
                .measurementIterations(3)
                .addProfiler(StackProfiler.class, "lines=100;top=2;period=10;detailLine=true")
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public void run1() throws UnknownHostException {
        System.out.println(InetAddress.getLocalHost());
    }

    @Benchmark
    public void run2() throws UnknownHostException {
        System.out.println(InetAddress.getLocalHost().getHostName());
    }
}
