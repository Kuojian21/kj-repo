package com.kj.repo.benchmark.jit;

import static com.kj.repo.infra.utils.el.OgnlUtil.DEFAULT_CLASS_RESOLVER;
import static com.kj.repo.infra.utils.el.OgnlUtil.DEFAULT_MEMBER_ACCESS;
import static com.kj.repo.infra.utils.el.OgnlUtil.DEFAULT_TYPE_CONVERTER;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
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
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.CompilerProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.WarmupMode;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.kj.repo.infra.utils.el.MvelUtil;
import com.kj.repo.infra.utils.el.OgnlUtil;

import ognl.Ognl;
import ognl.OgnlContext;

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
    private ConcurrentMap<String, Double> map = Maps.newConcurrentMap();

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
                .threads(3)
                .timeout(TimeValue.seconds(3))
                .syncIterations(true)
                .addProfiler(CompilerProfiler.class)
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public void random(Blackhole bh) {
        bh.consume(40.2143 + ThreadLocalRandom.current().nextInt(1000));
        bh.consume(50.4324 + ThreadLocalRandom.current().nextInt(1000));
        bh.consume(56.4321 + ThreadLocalRandom.current().nextInt(1000));
        bh.consume(54.5353 + ThreadLocalRandom.current().nextInt(1000));
    }

    @Benchmark
    public void randomMap(Blackhole bh) {
        bh.consume(ImmutableMap.of(
                "lat1", 40.2143 + ThreadLocalRandom.current().nextInt(1000),
                "lng1", 50.4324 + ThreadLocalRandom.current().nextInt(1000),
                "lat2", 56.4321 + ThreadLocalRandom.current().nextInt(1000),
                "lng2", 54.5353 + ThreadLocalRandom.current().nextInt(1000)
        ));
    }

    @Benchmark
    public void randomMapOgnlContext(Blackhole bh) {
        OgnlContext ognlContext =
                (OgnlContext) Ognl.createDefaultContext(this, DEFAULT_MEMBER_ACCESS, DEFAULT_CLASS_RESOLVER,
                        DEFAULT_TYPE_CONVERTER);
        ImmutableMap.of(
                "lat1", 40.2143 + ThreadLocalRandom.current().nextInt(1000),
                "lng1", 50.4324 + ThreadLocalRandom.current().nextInt(1000),
                "lat2", 56.4321 + ThreadLocalRandom.current().nextInt(1000),
                "lng2", 54.5353 + ThreadLocalRandom.current().nextInt(1000)
        ).forEach(ognlContext::put);
        bh.consume(ognlContext);
    }

    @Benchmark
    public void randomMapOgnl(Blackhole bh) {
        Map<String, Object> params =
                ImmutableMap.of(
                        "lat1", 40.2143 + ThreadLocalRandom.current().nextInt(1000),
                        "lng1", 50.4324 + ThreadLocalRandom.current().nextInt(1000),
                        "lat2", 56.4321 + ThreadLocalRandom.current().nextInt(1000),
                        "lng2", 54.5353 + ThreadLocalRandom.current().nextInt(1000)
                );
        bh.consume(OgnlUtil.execute(this, params, "distance(#lat1, #lng1, #lat2, #lng2)", 0d));
    }

    @Benchmark
    public void randomDriect(Blackhole bh) {
        bh.consume(distance(
                40.2143 + ThreadLocalRandom.current().nextInt(1000),
                50.4324 + ThreadLocalRandom.current().nextInt(1000),
                56.4321 + ThreadLocalRandom.current().nextInt(1000),
                54.5353 + ThreadLocalRandom.current().nextInt(1000))
        );
    }

    @Benchmark
    public void randomMvel(Blackhole bh) {
        Map<String, Object> vars =
                ImmutableMap.of(
                        "lat1", 40.2143 + ThreadLocalRandom.current().nextInt(1000),
                        "lng1", 50.4324 + ThreadLocalRandom.current().nextInt(1000),
                        "lat2", 56.4321 + ThreadLocalRandom.current().nextInt(1000),
                        "lng2", 54.5353 + ThreadLocalRandom.current().nextInt(1000),
                        "it", this
                );
        bh.consume(MvelUtil.execute("it.distance(lat1,lng1,lat2,lng2)", vars));
    }

    @Benchmark
    public void randomExecuteMap(Blackhole bh) {
        double lat1 = 40.2143 + ThreadLocalRandom.current().nextInt(1);
        double lng1 = 50.4324 + ThreadLocalRandom.current().nextInt(1);
        double lat2 = 56.4321 + ThreadLocalRandom.current().nextInt(1);
        double lng2 = 54.5353 + ThreadLocalRandom.current().nextInt(1);
        bh.consume(map.computeIfAbsent(lat1 + "#" + lng1 + "#" + lat2 + "#" + lng2,
                key -> distance(lat1, lng1, lat2, lng2)));
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