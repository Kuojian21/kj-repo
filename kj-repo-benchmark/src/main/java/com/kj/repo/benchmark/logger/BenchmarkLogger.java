package com.kj.repo.benchmark.logger;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.slf4j.Logger;

import com.kj.repo.infra.logger.Log4jHelper;
import com.kj.repo.infra.logger.Slf4jHelper;

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
        Log4jHelper.initialize("logger-log4j2.properties");
        Slf4jHelper.initialize("logger-logback.xml");
        log4j2Sync = Log4jHelper.syncLogger();
        log4j2Async = Log4jHelper.asyncLogger();
        logbackSync = Slf4jHelper.syncLogger();
        logbackAsync = Slf4jHelper.asyncLogger();
        System.out.println(Log4jHelper.getName());
        System.out.println(Slf4jHelper.getName());
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BenchmarkLogger.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .warmupBatchSize(1)
                .warmupTime(TimeValue.seconds(3))
                .measurementIterations(1)
                .measurementBatchSize(1)
                .measurementTime(TimeValue.minutes(3))
                .threads(Runtime.getRuntime().availableProcessors())
                .timeout(TimeValue.minutes(1))
                .syncIterations(true)
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
