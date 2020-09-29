package com.kj.repo.benchmark.share;

import static com.kj.repo.infra.share.ShareRepository.DEFAULT_LOAD_BATCH_FACTOR;
import static com.kj.repo.infra.share.ShareRepository.DEFAULT_LOAD_BATCH_SIZE;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.slf4j.Logger;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.alibaba.fastjson.JSON;
import com.github.phantomthief.util.MoreSuppliers;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import com.kj.repo.benchmark.share.model.Data;
import com.kj.repo.infra.logger.LogbackHelper;
import com.kj.repo.infra.perf.PerfHelper;
import com.kj.repo.infra.share.ShareClient;
import com.kj.repo.infra.share.ShareRepository;
import com.kj.repo.infra.share.ShareThreadFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;

/**
 * @author kj
 * Created on 2020-08-30
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BenchmarkShare {
    public static final Supplier<Executor> EXECUTOR10 =
            MoreSuppliers.lazy(MoreExecutors::directExecutor);
    public static final Supplier<Executor> EXECUTOR11 =
            MoreSuppliers.lazy(() -> Executors.newFixedThreadPool(100, new ShareThreadFactory()));
    public static final Supplier<Executor> EXECUTOR12 =
            MoreSuppliers.lazy(() -> new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(100), new ShareThreadFactory(), new CallerRunsPolicy()));
    public static final Supplier<Executor> EXECUTOR13 =
            MoreSuppliers.lazy(() -> new ThreadPoolExecutor(100, 100, 0L, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(1000), new ShareThreadFactory(), new CallerRunsPolicy()));
    public static long sleepNano = 0;
    private final Supplier<NamedParameterJdbcTemplate[]> shards;
    private final Supplier<Map<Long, List<Long>>> map;
    private final int shard = 100;
    private final boolean mac = "Mac OS X".equals(System.getProperty("os.name"));
    private RowMapper<Data> rowMapper = new BeanPropertyRowMapper<>(Data.class);
    private ShareRepository<Long, Long, Data> repo10 =
            ShareRepository.repo((sKey, ids) -> this.run(sKey, ids, "run10"), this::shard, EXECUTOR10,
                    DEFAULT_LOAD_BATCH_SIZE, DEFAULT_LOAD_BATCH_FACTOR, 0, null);
    private ShareRepository<Long, Long, Data> repo11 =
            ShareRepository.repo((sKey, ids) -> this.run(sKey, ids, "run11"), this::shard, EXECUTOR11,
                    DEFAULT_LOAD_BATCH_SIZE, DEFAULT_LOAD_BATCH_FACTOR, 0, null);
    private ShareRepository<Long, Long, Data> repo12 =
            ShareRepository.repo((sKey, ids) -> this.run(sKey, ids, "run12"), this::shard, EXECUTOR12,
                    DEFAULT_LOAD_BATCH_SIZE, DEFAULT_LOAD_BATCH_FACTOR, 0, null);
    private ShareRepository<Long, Long, Data> repo10NoFair =
            ShareRepository.repo((sKey, ids) -> this.run(sKey, ids, "run10-no-fair"), this::shard, EXECUTOR10,
                    DEFAULT_LOAD_BATCH_SIZE, DEFAULT_LOAD_BATCH_FACTOR, 0, new ReentrantLock());
    private ShareRepository<Long, Long, Data> repo11NoFair =
            ShareRepository.repo((sKey, ids) -> this.run(sKey, ids, "run11-no-fair"), this::shard, EXECUTOR11,
                    DEFAULT_LOAD_BATCH_SIZE, DEFAULT_LOAD_BATCH_FACTOR, 0, new ReentrantLock());
    private ShareRepository<Long, Long, Data> repo10Fair =
            ShareRepository.repo((sKey, ids) -> this.run(sKey, ids, "run10-fair"), this::shard, EXECUTOR10,
                    DEFAULT_LOAD_BATCH_SIZE, DEFAULT_LOAD_BATCH_FACTOR, 0, new ReentrantLock(true));
    private ShareRepository<Long, Long, Data> repo11Fair =
            ShareRepository.repo((sKey, ids) -> this.run(sKey, ids, "run11-fair"), this::shard, EXECUTOR11,
                    DEFAULT_LOAD_BATCH_SIZE, DEFAULT_LOAD_BATCH_FACTOR, 0, new ReentrantLock(true));
    @Param({"10"})
    private int count;
    @Param({"0", "1"})
    private int flag;
    private Logger logger = LogbackHelper.getLogger();
    private Consumer<Object> consumer;

    public BenchmarkShare() {
        shards = MoreSuppliers.lazy(() -> new NamedParameterJdbcTemplate[] {
        });
        map = mac ? MoreSuppliers.lazy(this::get3) : MoreSuppliers.lazy(this::get2);
        System.out.println("map-size:" + ObjectSizeCalculator.getObjectSize(map.get()));
    }

    public static void main(String[] args) throws RunnerException {
        System.out.println(Runtime.getRuntime().availableProcessors());
        int thread = 100;
        if (args.length > 0) {
            thread = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            sleepNano = Long.parseLong(args[1]);
        }
        Options opt = new OptionsBuilder()
                .include(BenchmarkShare.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .warmupBatchSize(1)
                .warmupTime(TimeValue.seconds(10))
                .measurementIterations(1)
                .measurementBatchSize(1)
                .measurementTime(TimeValue.minutes(3))
                .threads(thread)
                .timeout(TimeValue.hours(1))
                .syncIterations(true)
                .addProfiler(StackProfiler.class, "lines=30;top=10;detailLine=true")
                .build();
        new Runner(opt).run();
    }

    //    @Benchmark
    public void run0() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        consumer.accept(repo10.client(ids(false)));
        Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MICROSECONDS);
        PerfHelper.perf("share", "run0").count(1).micro(stopwatch.elapsed(TimeUnit.MICROSECONDS)).logstash();
    }

    @Benchmark
    public void run01() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ids(false).stream().flatMap(id -> this.shard(id).stream().map(sKey -> Pair.of(sKey, id)))
                .collect(Collectors.groupingBy(Pair::getKey))
                .forEach((sKey, keys) -> consumer
                        .accept(run(sKey, keys.stream().map(Pair::getValue).collect(Collectors.toSet()), "run01")));
        PerfHelper.perf("share", "run01").count(1).micro(stopwatch.elapsed(TimeUnit.MICROSECONDS)).logstash();
    }

    @Benchmark
    public void run10() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ShareClient<Long, Long, Data> client = repo10.client(ids(false));
        consumer.accept(client.get());
        PerfHelper.perf("share", "run10").count(1).micro(stopwatch.elapsed(TimeUnit.MICROSECONDS)).logstash();
    }

    @Benchmark
    public void run11() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ShareClient<Long, Long, Data> client = repo11.client(ids(false));
        consumer.accept(client.get());
        PerfHelper.perf("share", "run11").count(1).micro(stopwatch.elapsed(TimeUnit.MICROSECONDS))
                .logstash();
    }

    @Benchmark
    public void run12() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ShareClient<Long, Long, Data> client = repo12.client(ids(false));
        consumer.accept(client.get());
        PerfHelper.perf("share", "run12").count(1).micro(stopwatch.elapsed(TimeUnit.MICROSECONDS))
                .logstash();
    }

    //    @Benchmark
    public void run10Fair() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ShareClient<Long, Long, Data> client = repo10Fair.client(ids(false));
        consumer.accept(client.get());
        PerfHelper.perf("share", "run10").count(1).micro(stopwatch.elapsed(TimeUnit.MICROSECONDS)).logstash();
    }

    //    @Benchmark
    public void run11Fair() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ShareClient<Long, Long, Data> client = repo11Fair.client(ids(false));
        consumer.accept(client.get());
        PerfHelper.perf("share", "run11").count(1).micro(stopwatch.elapsed(TimeUnit.MICROSECONDS))
                .logstash();
    }

    //    @Benchmark
    public void run10NoFair() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ShareClient<Long, Long, Data> client = repo10NoFair.client(ids(false));
        consumer.accept(client.get());
        PerfHelper.perf("share", "run10").count(1).micro(stopwatch.elapsed(TimeUnit.MICROSECONDS)).logstash();
    }

    //    @Benchmark
    public void run11NoFair() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ShareClient<Long, Long, Data> client = repo11NoFair.client(ids(false));
        consumer.accept(client.get());
        PerfHelper.perf("share", "run11").count(1).micro(stopwatch.elapsed(TimeUnit.MICROSECONDS))
                .logstash();
    }

    @Benchmark
    public void run2() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Map<Long, Data> rtn = Maps.newHashMap();
        ids(false).stream().flatMap(id -> this.shard(id).stream().map(sKey -> Pair.of(sKey, id)))
                .collect(Collectors.groupingBy(Pair::getKey))
                .entrySet().stream()
                .map(entry -> ((ExecutorService) EXECUTOR11.get())
                        .submit(() -> run(entry.getKey(),
                                entry.getValue().stream().map(Pair::getValue).collect(Collectors.toSet()), "run2")))
                .collect(Collectors.toList())
                .forEach(f -> rtn.putAll(getUnchecked(f)));
        consumer.accept(rtn);
        PerfHelper.perf("share", "run2").count(1).micro(stopwatch.elapsed(TimeUnit.MICROSECONDS)).logstash();
    }

    //    @Benchmark
    public void run3() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Set<Long> ids = ids(true);
        Map<Long, Data> rtn = run(ids.iterator().next() % shard, ids, "run3");
        consumer.accept(rtn);
        PerfHelper.perf("share", "run3").count(1).micro(stopwatch.elapsed(TimeUnit.MICROSECONDS)).logstash();
    }

    @Setup
    public void setup() {
        consumer = flag == 0 ? new Blackhole(
                "Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.")::consume
                             : obj -> logger.info("{}", JSON.toJSONString(obj));
    }

    @TearDown
    public void down() {
    }

    public Map<Long, Data> run(long sKey, Set<Long> ids, String tag) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new RuntimeException();
        }
        Stopwatch stopwatch = Stopwatch.createStarted();
        Map<Long, Data> rtn = Maps.newHashMap();
        if (mac) {
            Uninterruptibles.sleepUninterruptibly(100 + 10 * ids.size(), TimeUnit.MICROSECONDS);
        } else {
            rtn = shards.get()[(int) (sKey % 5)]
                    .query("select id,global_id,source_key,source_id,status,extra,create_time,update_time from "
                                    + "ares_mini_data_2_" + sKey + " where global_id in(:globalIds)",
                            new MapSqlParameterSource("globalIds", ids), this.rowMapper)
                    .stream().collect(Collectors.toMap(Data::getGlobalId, Function.identity()));
        }
        PerfHelper.perf("share", tag, "count").count(1).micro(ids.size()).logstash();
        PerfHelper.perf("share", tag, "elapsed").count(1).micro(stopwatch.elapsed(TimeUnit.MICROSECONDS))
                .logstash();
        PerfHelper.perf("share", tag, "avg").count(ids.size()).micro(stopwatch.elapsed(TimeUnit.MICROSECONDS))
                .logstash();
        return rtn;
    }

    public Set<Long> ids(boolean common) {
        Set<Long> rtn = Sets.newHashSet();
        if (common) {
            List<Long> t = this.map.get().get(0L);
            while (rtn.size() < count) {
                rtn.add(t.get(ThreadLocalRandom.current().nextInt(t.size())));
            }
        } else {
            while (rtn.size() < count) {
                List<Long> t = this.map.get().get(ThreadLocalRandom.current().nextLong(this.map.get().size()));
                rtn.add(t.get(ThreadLocalRandom.current().nextInt(t.size())));
            }
        }
        return rtn;
    }

    public Set<Long> shard(Long l) {
        return Sets.newHashSet(l % shard);
    }

    public <V> V getUnchecked(Future<V> future) {
        try {
            return future.get();
        } catch (Exception var3) {
            throw new RuntimeException(var3);
        }
    }

    public Map<Long, List<Long>> get2() {
        Map<Long, List<Long>> rtn = Maps.newHashMap();
        for (long i = 0; i < shard; i++) {
            String sql = "select global_id from ares_mini_data_2_" + i + " limit 10000";
            rtn.put(i, shards.get()[(int) (i % 5)].queryForList(sql, new MapSqlParameterSource(), Long.class));
        }
        return rtn;
    }

    public Map<Long, List<Long>> get3() {
        Map<Long, List<Long>> rtn = Maps.newHashMap();
        for (long i = 0; i < 100; i++) {
            rtn.put(0L,
                    LongStream.range(0L, 1000000L / shard).boxed()
                            .map(l -> ThreadLocalRandom.current().nextLong(Long.MAX_VALUE))
                            .collect(Collectors.toList()));
        }
        return rtn;
    }

    public NamedParameterJdbcTemplate jdbcTemplate(String host, String port, String username,
            String password, String db) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(
                String.format("jdbc:mysql://%s:%s/%s?useUnicode=true&autoReconnectForPools=true&useCompression"
                        + "=true&rewriteBatchedStatements=true&useConfigs=maxPerformance&useSSL=false&useAffectedRows"
                        + "=true", host, port, db));
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(100);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        HikariDataSource ds = new HikariDataSource(config);
        NamedParameterJdbcTemplate rtn = new NamedParameterJdbcTemplate(ds);
        rtn.getJdbcTemplate().setFetchSize(100);
        return rtn;
    }
}