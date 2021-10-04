package com.kj.repo.infra.dao;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kj.repo.infra.dao.model.Expr;
import com.kj.repo.infra.dao.model.IModel;
import com.kj.repo.infra.dao.model.Pair;
import com.kj.repo.infra.dao.model.ShardHolder;
import com.kj.repo.infra.dao.model.SqlParams;
import com.kj.repo.infra.dao.sql.ParamsBuilder;
import com.kj.repo.infra.dao.sql.SqlBuilder;
import com.kj.repo.infra.logger.LoggerHelper;

/**
 * @author kj
 */
@SuppressWarnings({"unchecked"})
public abstract class DAO<T> {

    private static final Logger logger = LoggerHelper.getLogger();
    private final Class<T> clazz;
    private final RowMapper<T> rowMapper;
    private final IModel model;

    protected DAO() {
        this.clazz = (Class<T>) (((ParameterizedType) this.getClass().getGenericSuperclass())
                .getActualTypeArguments()[0]);
        this.rowMapper = new BeanPropertyRowMapper<>(this.clazz);
        this.model = IModel.model(this.clazz);
    }

    protected DAO(Class<T> clazz) {
        this.clazz = clazz;
        this.rowMapper = new BeanPropertyRowMapper<>(this.clazz);
        this.model = IModel.model(this.clazz);
    }

    public int insert(List<T> objs) {
        return this.insert(objs, true);
    }

    public int insert(List<T> objs, boolean ignore) {
        if (objs == null || objs.isEmpty()) {
            return 0;
        }
        return this.update(this.shard(objs).entrySet().stream()
                .map(e -> this.sqlBuilder().insert(this.model, e.getKey(), e.getValue(), ignore)));
    }

    public int upsert(List<T> objs, Collection<String> names) {
        if (CollectionUtils.isEmpty(objs)) {
            return 0;
        }
        Map<String, Object> values = Maps.newHashMap();
        if (!CollectionUtils.isEmpty(names)) {
            values = this.model.getProperties(names).stream()
                    .collect(Collectors.toMap(p -> p.getName() + "#EXPR", p -> "values(" + p.getColumn() + ")"));
        }
        return this.upsert(objs, values);
    }

    public int upsert(List<T> objs, Map<String, Object> values) {
        if (objs == null || objs.isEmpty()) {
            return 0;
        }
        return this.update(this.shard(objs).entrySet().stream().map(e -> this.sqlBuilder().upsert(this.model,
                e.getKey(), e.getValue(), this.sqlBuilder().valueExprs(model, true, values))));
    }

    public int delete(Map<String, Object> params) {
        return this.delete(ParamsBuilder.ofAnd().with(params));
    }

    public int delete(ParamsBuilder paramsBuilder) {
        return this.update(this.shard(paramsBuilder).entrySet().stream()
                .map(e -> this.sqlBuilder().delete(e.getKey(), e.getValue())));
    }

    public int update(Map<String, Object> values, Map<String, Object> params) {
        return this.update(this.sqlBuilder().valueExprs(this.model, false, values), ParamsBuilder.ofAnd().with(params));
    }

    public int update(Map<String, Expr> values, ParamsBuilder paramsBuilder) {
        return this.update(this.shard(paramsBuilder).entrySet().stream()
                .map(e -> this.sqlBuilder().update(e.getKey(), values, e.getValue())));
    }

    public int update(SqlParams sqlParams) {
        logger.debug("sql:{}", sqlParams.getSql());
        return IntStream.of(sqlParams.getShardHolder().getWriter().batchUpdate(sqlParams.getSql().toString(),
                sqlParams.getParamsList().toArray(new Map[0]))).sum();
    }

    public int update(Stream<SqlParams> stream) {
        return stream.map(this::update).mapToInt(r -> r).sum();
    }

    public List<T> select(Map<String, Object> params) {
        return this.select(params, null, null, null);
    }

    public List<T> select(Map<String, Object> params, List<String> orders, Integer offset, Integer limit) {
        return this.select(null, params, orders, offset, limit, this.getRowMapper());
    }

    public <R> List<R> select(Collection<String> columns, Map<String, Object> params, RowMapper<R> rowMapper) {
        return this.select(columns, params, null, null, null, rowMapper);
    }

    public <R> List<R> select(Collection<String> columns, Map<String, Object> params, List<String> orders,
            Integer offset, Integer limit, RowMapper<R> rowMapper) {
        return this.select(columns, ParamsBuilder.ofAnd().with(params), null, orders, offset, limit, rowMapper);
    }

    public <R> List<R> select(Collection<String> columns, ParamsBuilder paramsBuilder, List<String> groups,
            List<String> orders, Integer offset, Integer limit, RowMapper<R> rowMapper) {
        return this.select(this.shard(paramsBuilder).entrySet().stream().map(e -> this.sqlBuilder().select(this.model,
                e.getKey(), columns, e.getValue(), groups, orders, offset, limit)), rowMapper);
    }

    public <R> List<R> select(SqlParams sqlParams, RowMapper<R> rowMapper) {
        logger.debug("sql:{}", sqlParams.getSql());
        return sqlParams.getShardHolder().getReader().query(sqlParams.getSql().toString(),
                sqlParams.getParamsList().get(0), rowMapper);
    }

    public <R> List<R> select(Stream<SqlParams> stream, RowMapper<R> rowMapper) {
        return stream.map(s -> this.select(s, rowMapper)).flatMap(Collection::stream).collect(Collectors.toList());
    }

    protected abstract NamedParameterJdbcTemplate getReader();

    protected abstract NamedParameterJdbcTemplate getWriter();

    protected IModel getModel() {
        return this.model;
    }

    protected Class<T> getClazz() {
        return this.clazz;
    }

    protected RowMapper<T> getRowMapper() {
        return this.rowMapper;
    }

    protected Function<Object, ShardHolder> shard() {
        throw new RuntimeException("not supported");
    }

    protected List<ShardHolder> shards() {
        throw new RuntimeException("not supported");
    }

    protected DBType dbType() {
        return DBType.MYSQL;
    }

    private SqlBuilder sqlBuilder() {
        return SqlBuilder.sqlBuilder(this.dbType());
    }

    private Map<ShardHolder, ParamsBuilder.Params> shard(ParamsBuilder paramsBuilder) {
        ParamsBuilder.Params params = paramsBuilder.build(this.model, "");
        Map<String, List<Expr>> tParams = params.getMajor();
        IModel.IProperty property = this.model.getModelProperty();
        if (property == null) {
            return Helper.newHashMap(new ShardHolder(this.model.getTable(), this.getReader(), this.getWriter()),
                    params);
        }
        List<Expr> paramList = tParams.get(property.getName());
        if (paramList == null || params.getConn() == ParamsBuilder.CONN.OR) {
            ShardHolder holder = this.shard().apply(null);
            if (holder != null) {
                return Helper.newHashMap(holder, params);
            }
            return this.shards().stream().collect(Collectors.toMap(t -> t, t -> params));
        } else if (paramList.size() == 1) {
            Expr param = paramList.get(0);
            switch ((Expr.PType) param.getType()) {
                case EQ:
                    return Helper.newHashMap(this.shard().apply(property.cast(param.getValue())), params);
                case IN:
                    return ((Collection<?>) param.getValue()).stream()
                            .map(e -> Pair.pair(this.shard().apply(property.cast(e)), e))
                            .collect(Collectors.groupingBy(Pair::getKey)).entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey,
                                    e -> params.copyWith(property.getName(), Lists.newArrayList(param.copyWith(
                                            e.getValue().stream().map(Pair::getValue).collect(Collectors.toList()))))));
                default:
                    return this.shards().stream().collect(Collectors.toMap(t -> t, t -> params));
            }
        } else {
            return this.shards().stream().collect(Collectors.toMap(t -> t, t -> params));
        }

    }

    private Map<ShardHolder, List<T>> shard(List<T> objs) {
        IModel.IProperty property = this.model.getModelProperty();
        if (property == null) {
            return Helper.newHashMap(new ShardHolder(this.model.getTable(), this.getReader(), this.getWriter()), objs);
        } else {
            return objs.stream()
                    .collect(Collectors.groupingBy(o -> this.shard().apply(property.cast(property.getOrInsertDef(o)))));
        }
    }

    /**
     * @author kj
     */
    public enum DBType {
        MYSQL
    }

    /**
     * @author kj
     */
    public static class Helper {

        public static <K, V> Map<K, V> newHashMap(Object... objs) {
            Map<K, V> result = Maps.newHashMap();
            for (int i = 0, len = objs.length; i < len; i += 2) {
                result.put((K) objs[i], (V) objs[i + 1]);
            }
            return result;
        }
    }

}
