package com.kj.repo.benchmark.share;

import java.util.List;
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
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.github.phantomthief.util.MoreSuppliers;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.kj.repo.infra.helper.RunHelper;
import com.kj.repo.infra.perf.PerfHelper;
import com.kj.repo.infra.share.ShareClient;
import com.kj.repo.infra.share.ShareRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * @author kj
 * Created on 2020-08-30
 */
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BenchmarkShare {
    public static void main(String[] args) throws RunnerException {
        System.out.println(Runtime.getRuntime().availableProcessors());
        Options opt = new OptionsBuilder()
                .include(BenchmarkShare.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .warmupBatchSize(1)
                .warmupTime(TimeValue.seconds(50))
                .measurementIterations(1)
                .measurementBatchSize(1)
                .measurementTime(TimeValue.minutes(3))
                .threads(Runtime.getRuntime().availableProcessors() * 30)
                .timeout(TimeValue.hours(1))
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
    private NamedParameterJdbcTemplate jdbcTemplate = getJdbcTemplate();
    private RowMapper<Data> rowMapper = new BeanPropertyRowMapper<>(Data.class);
    private ShareRepository<Data> repo = ShareRepository.repo(this::run, this::shard);

    //            @Benchmark
    public void run0(Blackhole bh) {
        bh.consume(repo.client(ids()));
    }

    @Benchmark
    public void run1(Blackhole bh) throws InterruptedException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ShareClient<Data> client = repo.client(ids());
        Thread.sleep(10);
        bh.consume(client.get());
        PerfHelper.perf("share", "run1").count(1)
                .micro(stopwatch.elapsed(TimeUnit.MICROSECONDS)).logstash();
    }

    @Benchmark
    public void run2(Blackhole bh) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Map<Long, Data> rtn = Maps.newHashMap();
        ids().stream().collect(Collectors.groupingBy(this::shard, Collectors.toSet()))
                .entrySet().stream().map(e -> executor.get().submit(() -> run(e.getValue())))
                .collect(Collectors.toList())
                .forEach(f -> rtn.putAll(RunHelper.run((Callable<Map<Long, Data>>) f::get)));
        bh.consume(rtn);
        PerfHelper.perf("share", "run2").count(1)
                .micro(stopwatch.elapsed(TimeUnit.MICROSECONDS)).logstash();
    }

    @Setup
    public void setup() {
    }

    @TearDown
    public void down() {
    }

    public Map<Long, Data> run(Set<Long> ids) {
        List<Long> globalIds = Lists.newArrayList(ids);
        return jdbcTemplate
                .query("select * from ares_mini_data_" + shard(globalIds.get(0)) + " where global_id in(:globalIds)",
                        new MapSqlParameterSource("globalIds", globalIds), this.rowMapper).stream()
                .collect(Collectors.toMap(Data::getGlobalId, Function.identity()));
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

    public NamedParameterJdbcTemplate getJdbcTemplate() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(
                String.format("jdbc:mysql://%s:%s/%s?useUnicode=true&autoReconnectForPools=true&useCompression"
                        + "=true&rewriteBatchedStatements=true&useConfigs=maxPerformance&useSSL=false&useAffectedRows"
                        + "=true", "", "", ""));
        config.setUsername("");
        config.setPassword("");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        HikariDataSource ds = new HikariDataSource(config);
        return new NamedParameterJdbcTemplate(ds);
    }
}

/**
 * @author kj
 * Created on 2020-04-01
 */
class Data {
    private long id;
    private long createTime;
    private long updateTime;
    private long globalId;
    private String data;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public long getGlobalId() {
        return globalId;
    }

    public void setGlobalId(long globalId) {
        this.globalId = globalId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}



