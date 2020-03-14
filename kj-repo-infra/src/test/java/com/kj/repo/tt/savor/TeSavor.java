package com.kj.repo.tt.savor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.kj.repo.infra.savor.Savor;
import com.kj.repo.infra.savor.annotation.Model;
import com.kj.repo.infra.savor.annotation.Property;
import com.kj.repo.infra.savor.model.ShardHolder;
import com.kj.repo.infra.savor.sql.ParamsBuilder;
import com.mysql.cj.jdbc.Driver;

@SuppressWarnings("unchecked")
public class TeSavor {

    private static Logger log = LoggerFactory.getLogger(Savor.class);

    public static void main(String[] args) throws SQLException {
        /*
         * -DsocksProxyHost= -DsocksProxyPort=8088
         */
        //		System.setProperty("socksProxyHost", "127.0.0.1");
        //		System.setProperty("socksProxyPort", "8088");
        MDC.put("UUID", UUID.randomUUID().toString());
        MDC.get("UUID");
        TeSavor teSavor = new TeSavor();
        //        SavorTestDao<SavorTest> dao = new SavorTestDao<>(
        //                new SimpleDriverDataSource(new Driver(), args[0], args[1], args[2]),
        //                SavorTest.class);
        SavorTestDao<SavorShardTest> dao = new SavorTestDao<>(
                new SimpleDriverDataSource(new Driver(), args[0], args[1], args[2]),
                new SimpleDriverDataSource(new Driver(), args[3], args[4], args[5]), SavorShardTest.class);
        teSavor.insert(dao);
        teSavor.update(dao);
        teSavor.upsert(dao);
        teSavor.select(dao);
        teSavor.delete(dao);
    }

    public <T extends SavorTest> void insert(SavorTestDao<T> dao) {
        Random random = new Random();
        LongStream.range(0, 10).forEach(i -> {
            dao.insert((List<T>) LongStream.range(0, 10).boxed()
                    .map(j -> new SavorShardTest(i + j * 10, random.nextLong() + "", null,
                            random.nextInt() % 10 == 0 ? "zkj" : null,
                            random.nextInt() % 10 <= 9 ? (random.nextInt() % 2 == 0 ? "male" : "female") : null,
                            random.nextInt(100)))
                    .collect(Collectors.toList()));
        });
        this.select("after-insert-1", dao, Savor.Helper.newHashMap());
        this.select("after-insert-1", dao, Savor.Helper.newHashMap("name", "zkj", "sex", "male"));
    }

    public void update(SavorTestDao<?> dao) {
        this.select("before-update", dao,
                Savor.Helper.newHashMap("id", IntStream.range(0, 10).boxed().collect(Collectors.toList())));
        IntStream.range(0, 5).boxed().forEach(i -> {
            Map<String, Object> params = Savor.Helper.newHashMap("id", 10);
            Map<String, Object> values = Savor.Helper.newHashMap("age#sub", 5, "name", "kj");
            log.info("update values:{} params:{} {}", values, params, dao.update(values, params));
        });
        this.select("after-update", dao, Savor.Helper.newHashMap("name", "kj"));
    }

    public <T extends SavorTest> void upsert(SavorTestDao<T> dao) throws SQLException {
        this.select("before-upsert", dao, Savor.Helper.newHashMap("id#>", 94));
        List<T> objs = (List<T>) LongStream.range(95, 96).boxed().map(SavorShardTest::new).collect(Collectors.toList());
        List<String> names = Lists.newArrayList("hashKey");
        log.info("upsert data:{} names:{} {}", objs, names, dao.upsert(objs, names));
        this.select("after-upsert", dao, Savor.Helper.newHashMap("id#GT", 94));
        Map<String, Object> values = Savor.Helper.newHashMap("age#+", 1);
        log.info("upsert data:{} values:{} {}", objs, values, dao.upsert(objs, values));
        this.select("after-upsert", dao, Savor.Helper.newHashMap("id#GT", 94));
        values = Savor.Helper.newHashMap("age#sub", 3);
        log.info("upsert data:{} values:{} {}", objs, values, dao.upsert(objs, values));
        this.select("after-upsert", dao, Savor.Helper.newHashMap("id#GT", 94));
    }

    public void select(SavorTestDao<?> dao) {
        List<String> columns = Lists.newArrayList("id", "sex", "count(1)");
        ParamsBuilder paramsBuilder = ParamsBuilder.ofAnd().with("sex", "male")
                .with(ParamsBuilder.ofOr().with("id#>=", 90).with("id#<=", 10));
        List<String> groups = Lists.newArrayList("id", "sex");
        List<String> orders = Lists.newArrayList("id#D");
        Integer offset = 0;
        Integer limit = 10;
        log.info("select columns:{} params:{} groups:{} orders:{} offset:{} limit:{} {}", columns, paramsBuilder,
                groups, orders, offset, limit,
                dao.select(columns, paramsBuilder, groups, orders, offset, limit, new RowMapper<List<Object>>() {
                    @Override
                    public List<Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return Lists.newArrayList(rs.getLong("id"), rs.getString("sex"), rs.getInt("count(1)"));
                    }
                }));
        this.select("select", dao, Savor.Helper.newHashMap("id#!", 10));
    }

    public void delete(SavorTestDao<?> dao) {
        this.select("delete-before", dao, Savor.Helper.newHashMap("id#<=", 10));
        IntStream.range(0, 11).boxed().forEach(i -> {
            if (new Random().nextInt() % 2 == 0) {
                dao.delete(Savor.Helper.newHashMap("id", i));
            }
        });
        this.select("delete-after", dao, Savor.Helper.newHashMap("id#<=", 10));
    }

    public void select(String module, SavorTestDao<?> dao, Map<String, Object> params) {
        log.info("{} params:{} {}", module, JSON.toJSONString(params), dao.select(params));
    }

    public static class SavorTestDao<T> extends Savor<T> {

        private final NamedParameterJdbcTemplate jdbcTemplate1;
        private final NamedParameterJdbcTemplate jdbcTemplate2;

        public SavorTestDao(DataSource dataSource1, DataSource dataSource2, Class<T> clazz) {
            super(clazz);
            this.jdbcTemplate1 = new NamedParameterJdbcTemplate(dataSource1);
            this.jdbcTemplate2 = new NamedParameterJdbcTemplate(dataSource2);
        }

        public SavorTestDao(DataSource dataSource1, Class<T> clazz) {
            super(clazz);
            this.jdbcTemplate1 = new NamedParameterJdbcTemplate(dataSource1);
            this.jdbcTemplate2 = null;
        }

        @Override
        public NamedParameterJdbcTemplate getReader() {
            return jdbcTemplate1;
        }

        @Override
        public NamedParameterJdbcTemplate getWriter() {
            return jdbcTemplate1;
        }

        @Override
        public Function<Object, ShardHolder> shard() {
            return (v) -> {
                if (v == null) {
                    return null;
                }
                Long id = (Long) v;
                id = id % 10;
                return new ShardHolder(super.getModel().getTable() + "_" + id,
                        id % 2 == 0 ? jdbcTemplate1 : jdbcTemplate2, id % 2 == 0 ? jdbcTemplate1 : jdbcTemplate2);
            };
        }

        @Override
        public List<ShardHolder> shards() {
            return LongStream.range(0, 10).boxed().map(i -> this.shard().apply(i)).collect(Collectors.toList());
        }

    }

    /**
     * @author kj
     */
    public static class SavorTest {
        /* 自增主键 */
        @Property(insert = true)
        private Long id;
        /* key */
        private String hashKey;
        /* value */
        private String value;
        /* name */
        private String name;
        /* sex */
        private String sex;
        /* age */
        private Integer age;
        /* 创建时间 */
        @Property(defInsert = "1")
        private Long createTime;
        /* 创建时间 */
        @Property(defUpdate = "1")
        private java.sql.Timestamp updateTime;

        public SavorTest(Long id, String hashKey, String value, String name, String sex, Integer age) {
            this.id = id;
            this.hashKey = hashKey;
            this.value = value;
            this.name = name;
            this.sex = sex;
            this.age = age;
        }

        public SavorTest(Long id) {
            this.id = id;
        }

        public SavorTest() {
        }

        @Override
        public String toString() {
            return JSON.toJSONString(this);
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getHashKey() {
            return hashKey;
        }

        public void setHashKey(String hashKey) {
            this.hashKey = hashKey;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSex() {
            return sex;
        }

        public void setSex(String sex) {
            this.sex = sex;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public Long getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Long createTime) {
            this.createTime = createTime;
        }

        public java.sql.Timestamp getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp updateTime) {
            this.updateTime = updateTime;
        }

    }

    /**
     * @author kj
     */
    @Model(shardKey = "id")
    public static class SavorShardTest extends SavorTest {
        public SavorShardTest() {

        }

        public SavorShardTest(Long id) {
            super(id);
        }

        public SavorShardTest(Long id, String hashKey, String value, String name, String sex, Integer age) {
            super(id, hashKey, value, name, sex, age);
        }
    }

    /**
     * <pre>
     create table savor_test(
     id bigint(20) unsigned not null primary key comment '自增主键',
     hash_key varchar(64) comment 'key',
     value varchar(128)
     comment 'value',
     name varchar(64) comment 'name',
     sex varchar(64) comment 'sex',
     age tinyint comment 'age',
     create_time bigint(20) comment '创建时间',
     update_time timestamp default now() comment '创建时间'
     )ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;
     * </pre>
     */

    /**
     * <pre>
     * create table savor_shard_test_0(
     id bigint(20) unsigned not null primary key comment '自增主键',
     hash_key varchar(64) comment 'key',
     value varchar(128)
     comment 'value',
     name varchar(64) comment 'name',
     sex varchar(64) comment 'sex',
     age tinyint comment 'age',
     create_time bigint(20) comment '创建时间',
     update_time timestamp default now() comment '创建时间'
     )ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;
     create table savor_shard_test_2(
     id bigint(20) unsigned not null primary key comment '自增主键',
     hash_key varchar(64) comment 'key',
     value varchar(128)
     comment 'value',
     name varchar(64) comment 'name',
     sex varchar(64) comment 'sex',
     age tinyint comment 'age',
     create_time bigint(20) comment '创建时间',
     update_time timestamp default now() comment '创建时间'
     )ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;
     create table savor_shard_test_4(
     id bigint(20) unsigned not null primary key comment '自增主键',
     hash_key varchar(64) comment 'key',
     value varchar(128)
     comment 'value',
     name varchar(64) comment 'name',
     sex varchar(64) comment 'sex',
     age tinyint comment 'age',
     create_time bigint(20) comment '创建时间',
     update_time timestamp default now() comment '创建时间'
     )ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;
     create table savor_shard_test_6(
     id bigint(20) unsigned not null primary key comment '自增主键',
     hash_key varchar(64) comment 'key',
     value varchar(128)
     comment 'value',
     name varchar(64) comment 'name',
     sex varchar(64) comment 'sex',
     age tinyint comment 'age',
     create_time bigint(20) comment '创建时间',
     update_time timestamp default now() comment '创建时间'
     )ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;
     create table savor_shard_test_8(
     id bigint(20) unsigned not null primary key comment '自增主键',
     hash_key varchar(64) comment 'key',
     value varchar(128)
     comment 'value',
     name varchar(64) comment 'name',
     sex varchar(64) comment 'sex',
     age tinyint comment 'age',
     create_time bigint(20) comment '创建时间',
     update_time timestamp default now() comment '创建时间'
     )ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;
     * </pre>
     */
    /**
     * <pre>
     * create table savor_shard_test_1(
     id bigint(20) unsigned not null primary key comment '自增主键',
     hash_key varchar(64) comment 'key',
     value varchar(128)
     comment 'value',
     name varchar(64) comment 'name',
     sex varchar(64) comment 'sex',
     age tinyint comment 'age',
     create_time bigint(20) comment '创建时间',
     update_time timestamp default now() comment '创建时间'
     )ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;
     create table savor_shard_test_3(
     id bigint(20) unsigned not null primary key comment '自增主键',
     hash_key varchar(64) comment 'key',
     value varchar(128)
     comment 'value',
     name varchar(64) comment 'name',
     sex varchar(64) comment 'sex',
     age tinyint comment 'age',
     create_time bigint(20) comment '创建时间',
     update_time timestamp default now() comment '创建时间'
     )ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;
     create table savor_shard_test_5(
     id bigint(20) unsigned not null primary key comment '自增主键',
     hash_key varchar(64) comment 'key',
     value varchar(128)
     comment 'value',
     name varchar(64) comment 'name',
     sex varchar(64) comment 'sex',
     age tinyint comment 'age',
     create_time bigint(20) comment '创建时间',
     update_time timestamp default now() comment '创建时间'
     )ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;
     create table savor_shard_test_7(
     id bigint(20) unsigned not null primary key comment '自增主键',
     hash_key varchar(64) comment 'key',
     value varchar(128)
     comment 'value',
     name varchar(64) comment 'name',
     sex varchar(64) comment 'sex',
     age tinyint comment 'age',
     create_time bigint(20) comment '创建时间',
     update_time timestamp default now() comment '创建时间'
     )ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;
     create table savor_shard_test_9(
     id bigint(20) unsigned not null primary key comment '自增主键',
     hash_key varchar(64) comment 'key',
     value varchar(128)
     comment 'value',
     name varchar(64) comment 'name',
     sex varchar(64) comment 'sex',
     age tinyint comment 'age',
     create_time bigint(20) comment '创建时间',
     update_time timestamp default now() comment '创建时间'
     )ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;
     * </pre>
     */

}
