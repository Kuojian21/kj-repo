package com.kj.repo.benchmark.share;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

import com.github.phantomthief.util.MoreSuppliers;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.kj.repo.infra.helper.RunHelper;
import com.kj.repo.infra.share.ShareClient;
import com.kj.repo.infra.share.ShareRepository;

/**
 * @author kj
 * Created on 2020-08-30
 */
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BenchmarkShare {
    public static void main(String[] args) throws RunnerException {
        System.out.println(Runtime.getRuntime().availableProcessors() * 5);
        Options opt = new OptionsBuilder()
                .include(BenchmarkShare.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .warmupBatchSize(1)
                .warmupTime(TimeValue.seconds(30))
                .measurementIterations(1)
                .measurementBatchSize(1)
                .measurementTime(TimeValue.minutes(3))
                .threads(Runtime.getRuntime().availableProcessors() * 5)
                .timeout(TimeValue.seconds(30))
                .syncIterations(true)
                .build();
        new Runner(opt).run();
    }

    private final Supplier<ExecutorService> executor = MoreSuppliers.lazy(() -> Executors.newFixedThreadPool(100, r -> {
        Thread thread = new Thread(r);
        thread.setName("share-pool");
        thread.setDaemon(true);
        return thread;
    }));
    private ShareRepository<Long> repo = ShareRepository.repo(this::run, this::shard);

    //        @Benchmark
    public void run0(Blackhole bh) throws InterruptedException {
        bh.consume(repo.client(ids()));
    }

    @Benchmark
    public void run1(Blackhole bh) throws InterruptedException {
        ShareClient<Long> client = repo.client(ids());
        bh.consume(check(client.get()));
    }

    @Benchmark
    public void run2(Blackhole bh) {
        Map<Long, Long> rtn = Maps.newHashMap();
        ids().stream().collect(Collectors.groupingBy(this::shard, Collectors.toSet()))
                .entrySet().stream().map(e -> executor.get().submit(() -> run(e.getValue())))
                .collect(Collectors.toList())
                .forEach(f -> rtn.putAll(RunHelper.run((Callable<Map<Long, Long>>) f::get)));
        bh.consume(check(rtn));
    }

    public Map<Long, Long> run(Set<Long> ids) {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }
        return ids.stream().collect(Collectors.toMap(Function.identity(), Function.identity()));
    }

    public Set<Long> ids() {
        Set<Long> rtn = Sets.newHashSet();
        while (rtn.size() < 10) {
            rtn.add(ThreadLocalRandom.current().nextLong(10000));
        }
        return rtn;
    }

    public Map<Long, Long> check(Map<Long, Long> result) {
        if (result.size() != 10) {
            System.out.println(result.size());
        }
        if (result.entrySet().stream().anyMatch(e -> !e.getKey().equals(e.getValue()))) {
            throw new RuntimeException();
        }
        return result;
    }

    public long shard(Long l) {
        return l % 100;
    }
}
