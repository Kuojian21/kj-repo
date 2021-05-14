package com.kj.repo.benchmark.clone;

import com.kj.repo.benchmark.common.BenchmarkCommon;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.WarmupMode;

import javax.swing.*;
import java.util.concurrent.TimeUnit;

/**
 * @author kj
 * Created on 2020-03-26
 */
@BenchmarkMode({Mode.AverageTime})
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class BenchmarkClone  implements Cloneable{
    private int a = 10;
    private int b = 10;
    private Label label = new Label();

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
                .build();
        new Runner(opt).run();
    }



    @Benchmark
    public void run1(Blackhole bh) {
        bh.consume(new Label());
    }

    @Benchmark
    public void run2(Blackhole bh) throws CloneNotSupportedException {
        bh.consume(label.clone());
    }

    static class Label extends JLabel implements Cloneable{
        public Object clone(){
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
