package com.kj.repo.benchmark.logger;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.slf4j.Logger;

import com.kj.repo.infra.logger.Log4j2Helper;
import com.kj.repo.infra.logger.LogbackHelper;

/**
 * @author kj
 * Created on 2020-03-18
 */
@BenchmarkMode({Mode.Throughput})
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BenchmarkLogger {
    private final org.apache.logging.log4j.Logger log4j2Sync;
    private final org.apache.logging.log4j.Logger log4j2Async;
    private final Logger logbackSync;
    private final Logger logbackAsync;

    public BenchmarkLogger() {
        Log4j2Helper.initialize("logger-log4j2.properties");
        LogbackHelper.initialize("logger-logback.xml");
        log4j2Sync = Log4j2Helper.syncLogger();
        log4j2Async = Log4j2Helper.asyncLogger();
        logbackSync = LogbackHelper.syncLogger();
        logbackAsync = LogbackHelper.asyncLogger();
    }

    public static void main(String[] args) throws RunnerException {
        System.out.println(Runtime.getRuntime().availableProcessors() / 2);
        Options opt = new OptionsBuilder()
                .include(BenchmarkLogger.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .warmupBatchSize(1)
                .warmupTime(TimeValue.seconds(3))
                .measurementIterations(1)
                .measurementBatchSize(1)
                .measurementTime(TimeValue.minutes(1))
                .threads(Runtime.getRuntime().availableProcessors() / 2)
                .timeout(TimeValue.seconds(30))
                .syncIterations(true)
                .addProfiler(StackProfiler.class, "lines=30;top=6;period=10;detailLine=true")
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public void log4jSync() {
        log4j2Sync.error("{}", System.currentTimeMillis());
    }

    @Benchmark
    public void log4jAsync() {
        log4j2Async.error("{}", System.currentTimeMillis());
    }

    @Benchmark
    public void logbackSync() {
        logbackSync.error("{}", System.currentTimeMillis());
    }

    @Benchmark
    public void logbackAsync() {
        logbackAsync.error("{}", System.currentTimeMillis());
    }

    @Setup
    public void setup() {

    }

}
