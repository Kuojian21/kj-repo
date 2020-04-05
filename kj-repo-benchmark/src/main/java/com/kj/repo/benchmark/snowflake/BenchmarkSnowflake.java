package com.kj.repo.benchmark.snowflake;

/**
 * @author kj
 * Created on 2020-04-02
 */

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

import com.kj.repo.infra.algorithm.Snowflake;

/**
 * @author kj
 * Created on 2020-03-21
 */
@BenchmarkMode({Mode.Throughput})
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BenchmarkSnowflake {

    private Snowflake snowflake = new Snowflake(0L, 0L);

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BenchmarkSnowflake.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .warmupBatchSize(1)
                .warmupTime(TimeValue.seconds(1))
                .warmupMode(WarmupMode.INDI)
                .measurementIterations(1)
                .measurementBatchSize(1)
                .measurementTime(TimeValue.seconds(60))
                .threads(Runtime.getRuntime().availableProcessors() / 2)
                .timeout(TimeValue.seconds(3))
                .syncIterations(true)
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public void run(Blackhole bh) {
        bh.consume(snowflake.nextId());
    }


}
