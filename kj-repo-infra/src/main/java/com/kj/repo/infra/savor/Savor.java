package com.kj.repo.infra.savor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kj.repo.infra.savor.Savor.Expr.PType;
import com.kj.repo.infra.savor.Savor.Expr.Type;
import com.kj.repo.infra.savor.Savor.ParamsBuilder.Params;

import lombok.Getter;

/**
 * @author kuojian21
 */
@SuppressWarnings({"checkstyle:HiddenField", "checkstyle:ParameterNumber", "unchecked"})
public abstract class Savor<T> {

    private static Logger logger = LoggerFactory.getLogger(Savor.class);
    private final Class<T> clazz;
    private final RowMapper<T> rowMapper;
    private final Model model;

    protected Savor() {
        this.clazz = (Class<T>) (((ParameterizedType) this.getClass().getGenericSuperclass())
                .getActualTypeArguments()[0]);
        this.rowMapper = new BeanPropertyRowMapper<>(this.clazz);
        this.model = Model.model(this.clazz);
    }

    protected Savor(Class<T> clazz) {
        this.clazz = clazz;
        this.rowMapper = new BeanPropertyRowMapper<>(this.clazz);
        this.model = Model.model(this.clazz);
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
        logger.info("sql:{}", sqlParams.getSql());
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
        logger.info("sql:{}", sqlParams.getSql());
        return sqlParams.getShardHolder().getReader().query(sqlParams.getSql().toString(),
                sqlParams.getParamsList().get(0), rowMapper);
    }

    public <R> List<R> select(Stream<SqlParams> stream, RowMapper<R> rowMapper) {
        return stream.map(s -> this.select(s, rowMapper)).flatMap(Collection::stream).collect(Collectors.toList());
    }

    protected abstract NamedParameterJdbcTemplate getReader();

    protected abstract NamedParameterJdbcTemplate getWriter();

    protected Model getModel() {
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
        ParamsBuilder.Params params = paramsBuilder.build(this.model);
        Map<String, List<Expr>> tParams = params.major;
        Property property = this.model.getShardProperty();
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
            switch ((PType) param.getType()) {
                case EQ:
                    return Helper.newHashMap(this.shard().apply(property.cast(param.getValue())), params);
                case IN:
                    return ((Collection<?>) param.getValue()).stream()
                            .map(e -> Tuple.tuple(this.shard().apply(property.cast(e)), e))
                            .collect(Collectors.groupingBy(Tuple::getX)).entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey,
                                    e -> params.copyWith(property.getName(), Lists.newArrayList(Expr.param(property,
                                            Expr.PType.IN,
                                            e.getValue().stream().map(Tuple::getY).collect(Collectors.toList()))))));
                default:
                    return this.shards().stream().collect(Collectors.toMap(t -> t, t -> params));
            }
        } else {
            return this.shards().stream().collect(Collectors.toMap(t -> t, t -> params));
        }

    }

    private Map<ShardHolder, List<T>> shard(List<T> objs) {
        Property property = this.model.getShardProperty();
        if (property == null) {
            return Helper.newHashMap(new ShardHolder(this.model.getTable(), this.getReader(), this.getWriter()), objs);
        } else {
            return objs.stream()
                    .collect(Collectors.groupingBy(o -> this.shard().apply(property.cast(property.getOrInsertDef(o)))));
        }
    }

    public enum DBType {
        MYSQL
    }

    /**
     * @author kuojian21
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Shard {
        String table() default "";

        String shardKey() default "";
    }

    /**
     * @author kuojian21
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PrimaryKey {
        boolean insert() default false;
    }

    /**
     * @author kuojian21
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TimeInsert {
        String value() default "";
    }

    /**
     * @author kuojian21
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TimeUpdate {
        String value() default "";
    }

    /**
     * @author kuojian21
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

    /**
     * @author kuojian21
     */
    @Getter
    public static class Model {

        private static final ConcurrentMap<Class<?>, Model> MODELS = Maps.newConcurrentMap();

        private final String name;
        private final String table;
        private final List<Property> properties;
        private final Map<String, Property> propertyMap;
        private final List<Property> insertProperties;
        private final Property shardProperty;
        private final List<Property> updateTimeProperties;

        public Model(Class<?> clazz) {
            super();
            this.name = clazz.getSimpleName();
            Model pModel = Object.class.equals(clazz.getSuperclass()) ? null : Model.model(clazz.getSuperclass());
            List<Property> tProperties = Arrays.stream(clazz.getDeclaredFields())
                    .filter(f -> !Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers()))
                    .map(Property::new).collect(Collectors.toList());
            if (pModel != null) {
                tProperties.addAll(pModel.getProperties());
            }
            this.properties = Collections.unmodifiableList(tProperties);
            this.propertyMap = Collections.unmodifiableMap(properties.stream()
                    .map(p -> Lists.newArrayList(Tuple.tuple(p.getName(), p), Tuple.tuple(p.getColumn(), p)))
                    .flatMap(List::stream).distinct().collect(Collectors.toMap(Tuple::getX, Tuple::getY)));
            Shard shard = clazz.getAnnotation(Shard.class);
            if (shard == null) {
                this.table = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, this.name);
                this.shardProperty = null;
            } else {
                this.table = !Strings.isNullOrEmpty(shard.table()) ? shard.table()
                        : CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, this.name);
                this.shardProperty = !Strings.isNullOrEmpty(shard.shardKey()) ? this.getProperty(shard.shardKey())
                        : null;
            }
            this.updateTimeProperties = Collections.unmodifiableList(
                    Arrays.stream(clazz.getDeclaredFields()).filter(f -> f.getAnnotation(TimeUpdate.class) != null)
                            .map(f -> this.getProperty(f.getName())).collect(Collectors.toList()));
            this.insertProperties = Collections
                    .unmodifiableList(properties.stream().filter(Property::isInsertable).collect(Collectors.toList()));
        }

        public Property getProperty(String name) {
            return this.propertyMap.get(name);
        }

        public List<Property> getProperties(Collection<String> names) {
            return names.stream().map(this.propertyMap::get).collect(Collectors.toList());
        }

        public static Model model(Class<?> clazz) {
            return MODELS.computeIfAbsent(clazz, Model::new);
        }
    }

    /**
     * @author kuojian21
     */
    @Getter
    public static class Property {

        private final String name;
        private final String column;
        private final Class<?> type;
        private final Field field;
        private final boolean primaryKey;
        private final boolean insertable;
        private final Supplier<Object> insertDef;
        private final Supplier<Object> updateDef;

        public Property(Field f) {
            f.setAccessible(true);
            this.field = f;
            this.name = f.getName();
            this.column = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, f.getName());
            this.type = f.getType();
            this.primaryKey = f.getAnnotation(PrimaryKey.class) != null;
            this.insertable = f.getAnnotation(PrimaryKey.class) == null || f.getAnnotation(PrimaryKey.class).insert();
            this.insertDef = insertDef(f);
            this.updateDef = updateDef(f);
        }

        public Supplier<Object> insertDef(Field f) {
            TimeInsert inDef = f.getAnnotation(TimeInsert.class);
            if (inDef != null) {
                return parseDef(f.getType(), inDef.value());
            } else {
                return () -> null;
            }
        }

        public Supplier<Object> updateDef(Field f) {
            TimeUpdate upDef = f.getAnnotation(TimeUpdate.class);
            if (upDef != null) {
                return parseDef(f.getType(), upDef.value());
            } else {
                return () -> null;
            }
        }

        public Supplier<Object> parseDef(Class<?> type, String value) {
            if (Long.class.equals(type)) {
                return System::currentTimeMillis;
            } else if (java.sql.Date.class.equals(type)) {
                return () -> new java.sql.Date(System.currentTimeMillis());
            } else if (Timestamp.class.equals(type)) {
                return () -> new Timestamp(System.currentTimeMillis());
            }
            return () -> null;
        }

        public Object getOrInsertDef(Object obj) {
            try {
                Object rtn = this.field.get(obj);
                if (rtn == null) {
                    return this.insertDef.get();
                }
                return rtn;
            } catch (IllegalArgumentException | IllegalAccessException e) {
                logger.error("", e);
                return null;
            }
        }

        public Object cast(Object obj) {
            if (obj != null && !this.type.equals(obj.getClass())) {
                if (this.type == Long.class) {
                    return Long.parseLong(obj.toString());
                } else if (this.type == Integer.class) {
                    return Integer.parseInt(obj.toString());
                } else if (this.type == Short.class) {
                    return Short.parseShort(obj.toString());
                } else if (this.type == Byte.class) {
                    return Byte.parseByte(obj.toString());
                } else if (this.type == Double.class) {
                    return Double.parseDouble(obj.toString());
                } else if (this.type == Float.class) {
                    return Float.parseFloat(obj.toString());
                }
            }
            return obj;
        }

    }

    /**
     * @author kuojian21
     */
    @Getter
    public static class Tuple<X, Y> {
        private final X x;
        private final Y y;

        public Tuple(X x, Y v) {
            super();
            this.x = x;
            this.y = v;
        }

        public static <X, Y> Tuple<X, Y> tuple(X x, Y y) {
            return new Tuple<>(x, y);
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || other.getClass() != Tuple.class) {
                return false;
            }
            Tuple<?, ?> oTuple = (Tuple<?, ?>) other;
            if (this.x != null && !this.x.equals(oTuple.x)) {
                return false;
            } else if (this.x == null && oTuple.x != null) {
                return false;
            } else if (this.y != null && !this.y.equals(oTuple.y)) {
                return false;
            } else {
                return this.y != null || oTuple.y == null;
            }
        }

        @Override
        public int hashCode() {
            return (this.x == null ? 0 : this.x.hashCode()) / 2 + (this.y == null ? 0 : this.y.hashCode()) / 2;
        }
    }

    /**
     * @author kuojian21
     */
    @Getter
    public static class SqlParams {
        private final ShardHolder shardHolder;
        private final StringBuilder sql;
        private final List<Map<String, Object>> paramsList;

        public SqlParams(ShardHolder shardHolder, StringBuilder sql, List<Map<String, Object>> paramsList) {
            this.shardHolder = shardHolder;
            this.sql = sql;
            this.paramsList = paramsList;
        }

        public static SqlParams model(ShardHolder shardHolder, StringBuilder sql,
                                      List<Map<String, Object>> paramsList) {
            return new SqlParams(shardHolder, sql, paramsList);
        }

    }

    /**
     * @author kuojian21
     */
    @Getter
    public static class ShardHolder {

        private final String table;
        private final NamedParameterJdbcTemplate reader;
        private final NamedParameterJdbcTemplate writer;

        public ShardHolder(String table, NamedParameterJdbcTemplate reader, NamedParameterJdbcTemplate writer) {
            this.table = table;
            this.reader = reader;
            this.writer = writer;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof ShardHolder) {
                ShardHolder o = (ShardHolder) other;
                return this.table.equals(o.table);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.table.hashCode();
        }

        public NamedParameterJdbcTemplate getReader() {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                return this.writer;
            }
            return this.reader;
        }

    }

    /**
     * @author kuojian21
     */
    @Getter
    public static class Expr {
        private final Property property;
        private final Type type;
        private final String vName;
        private final String expr;
        private final Object value;

        public Expr(Property property, Type type, String vName, String expr, Object value) {
            super();
            this.property = property;
            this.type = type;
            this.vName = vName;
            this.expr = expr;
            this.value = value;
        }

        /**
         * @author kuojian21
         */
        public interface Type {
            String name();

            String symbol();
        }

        /**
         * @author kuojian21
         */
        @Getter
        public enum PType implements Type {
            EQ("="), IN("in"), LT("<"), LE("<="), GT(">"), GE(">="), NE("!=");
            private final String symbol;

            PType(String symbol) {
                this.symbol = symbol;
            }

            public String symbol() {
                return symbol;
            }
        }

        public static Expr param(Property p, PType type, Object value) {
            String vname = p.name + "#" + type.name();
            String expr;
            switch (type) {
                case IN:
                    expr = p.getColumn() + " in ( :" + vname + " )";
                    break;
                case LT:
                case LE:
                case GT:
                case GE:
                case NE:
                case EQ:
                    expr = p.getColumn() + " " + type.symbol + " :" + vname;
                    break;
                default:
                    throw new RuntimeException("not support");
            }
            return new Expr(p, type, vname, expr, value);
        }

    }

    /**
     * @author kuojian21
     */
    public static class ParamsBuilder {

        private final CONN conn;
        private final Map<String, Object> major = Maps.newHashMap();
        private final List<ParamsBuilder> minor = Lists.newArrayList();

        private ParamsBuilder(CONN conn) {
            super();
            this.conn = conn;
        }

        public static ParamsBuilder ofAnd() {
            return new ParamsBuilder(CONN.AND);
        }

        public static ParamsBuilder ofOr() {
            return new ParamsBuilder(CONN.OR);
        }

        public ParamsBuilder with(Map<String, Object> params) {
            this.major.putAll(params);
            return this;
        }

        public ParamsBuilder with(String name, Object value) {
            this.major.put(name, value);
            return this;
        }

        public ParamsBuilder with(ParamsBuilder pBuilder) {
            this.minor.add(pBuilder);
            return this;
        }

        public Params build(Model model) {
            Map<String, List<Expr>> result = Maps.newHashMap();
            if (!CollectionUtils.isEmpty(this.major)) {
                this.major.forEach((key, value) -> {
                    String[] s = key.split("#");
                    s[0] = s[0].trim();
                    Property p = model.getProperty(s[0]);
                    Expr.PType op = Expr.PType.EQ;
                    if (s.length == 2) {
                        switch (s[1].trim().toUpperCase()) {
                            case "<=":
                            case "LE":
                                op = Expr.PType.LE;
                                break;
                            case "<":
                            case "LT":
                                op = Expr.PType.LT;
                                break;
                            case ">=":
                            case "GE":
                                op = Expr.PType.GE;
                                break;
                            case ">":
                            case "GT":
                                op = Expr.PType.GT;
                                break;
                            case "!=":
                            case "!":
                            case "<>":
                            case "NE":
                                op = Expr.PType.NE;
                                break;
                            case "IN":
                            case "=":
                            case "EQ":
                                op = Expr.PType.EQ;
                                break;
                            default:
                                throw new RuntimeException("invalid syntax:" + key);
                        }
                    }
                    if (value instanceof Collection || value.getClass().isArray()) {
                        if (op != Expr.PType.EQ) {
                            throw new RuntimeException("invalid syntax:" + key);
                        } else if (value.getClass().isArray()) {
                            result.computeIfAbsent(p.getName(), k -> Lists.newArrayList())
                                    .add(Expr.param(p, Expr.PType.IN, Arrays.asList((Object[]) value)));
                        } else {
                            result.computeIfAbsent(p.getName(), k -> Lists.newArrayList())
                                    .add(Expr.param(p, Expr.PType.IN, value));
                        }
                    } else {
                        result.computeIfAbsent(p.getName(), k -> Lists.newArrayList()).add(Expr.param(p, op, value));
                    }
                });
            }
            Params tParams = new Params(this.conn, result);
            List<String> exprs = tParams.getMajor().entrySet().stream().flatMap(e -> e.getValue().stream())
                    .sorted(Comparator.comparing(Expr::getVName)).map(Expr::getExpr).collect(Collectors.toList());
            this.minor.stream().map(pb -> pb.build(model)).sorted(Comparator.comparing(p -> p.getWhere().toString()))
                    .forEach(p -> {
                        exprs.add("(" + p.getWhere() + ")");
                        tParams.minor.putAll(p.getMajor());
                        tParams.minor.putAll(p.getMinor());
                    });
            tParams.where.append(Joiner.on(" " + this.conn.name() + " ").join(exprs));
            return tParams;
        }

        /**
         * @author kuojian21
         */
        public enum CONN {
            AND, OR
        }

        /**
         * @author kuojian21
         */
        @Getter
        public static class Params {
            private final ParamsBuilder.CONN conn;
            private final StringBuilder where;
            private final Map<String, List<Expr>> major;
            private final Map<String, List<Expr>> minor;

            public Params(ParamsBuilder.CONN conn, Map<String, List<Expr>> major) {
                super();
                this.conn = conn;
                this.major = major;
                this.minor = Maps.newHashMap();
                this.where = new StringBuilder();
            }

            public Params(CONN conn, StringBuilder where, Map<String, List<Expr>> major,
                          Map<String, List<Expr>> minor) {
                this.conn = conn;
                this.where = where;
                this.major = major;
                this.minor = minor;
            }

            public Params copyWith(String name, List<Expr> paramList) {
                Map<String, List<Expr>> m = Maps.newHashMap(this.major);
                m.put(name, paramList);
                return new Params(this.conn, this.where, m, this.minor);
            }

            public String getSqlWhere() {
                if (this.where.length() <= 0) {
                    return "";
                }
                return " where " + this.where.toString();
            }

        }
    }

    /**
     * @author kuojian21
     */
    public static abstract class SqlBuilder {

        private static Logger logger = LoggerFactory.getLogger(Savor.class);
        private static final String NEW_VALUE_SUFFIX = "$newValueSuffix$";
        private static final String VAR_LIMIT = "$limit$";
        private static final String VAR_OFFSET = "$offset$";
        private static final ConcurrentMap<Savor.DBType, SqlBuilder> BUILDERS = Maps.newConcurrentMap();

        static {
            register(Savor.DBType.MYSQL, new MysqlBuilder());
        }

        /**
         * @author kuojian21
         */
        @Getter
        public enum VType implements Type {
            EQ("="), ADD("+"), SUB("-"), EXPR("EXPR");
            private final String symbol;

            VType(String symbol) {
                this.symbol = symbol;
            }

            public String symbol() {
                return symbol;
            }
        }

        public static SqlBuilder sqlBuilder(Savor.DBType dbType) {
            return BUILDERS.get(dbType);
        }

        public static void register(Savor.DBType dbType, SqlBuilder sqlBuilder) {
            BUILDERS.putIfAbsent(dbType, sqlBuilder);
        }

        public abstract Map<String, Expr> valueExprs(Model model, boolean upsert, Map<String, Object> values);

        public abstract <T> SqlParams insert(Model model, ShardHolder holder, List<T> objs, boolean ignore);

        public abstract <T> SqlParams upsert(Model model, ShardHolder holder, List<T> objs, Map<String, Expr> values);

        public abstract SqlParams select(Model model, ShardHolder holder, Collection<String> columns, Params params,
                                         List<String> groups, List<String> orders, Integer offset, Integer limit);

        public static Map<String, Object> expr(Map<String, Object> paramMap, ParamsBuilder.Params params) {
            params.major.forEach((key, value) -> value.forEach(v -> paramMap.put(v.getVName(), v.getValue())));
            params.minor.forEach((key, value) -> value.forEach(v -> paramMap.put(v.getVName(), v.getValue())));
            return paramMap;
        }

        public static Map<String, Object> expr(Map<String, Object> paramMap, Map<String, Expr> values) {
            values.values().forEach(v -> paramMap.put(v.getVName(), v.getValue()));
            return paramMap;
        }

        public SqlParams delete(ShardHolder holder, ParamsBuilder.Params params) {
            StringBuilder sql = new StringBuilder();
            sql.append("delete from ").append(holder.getTable()).append("\n").append(params.getSqlWhere());
            return SqlParams.model(holder, sql, Lists.newArrayList(expr(Maps.newHashMap(), params)));
        }

        public SqlParams update(ShardHolder holder, Map<String, Expr> values, ParamsBuilder.Params params) {
            if (CollectionUtils.isEmpty(values)) {
                logger.error("invalid syntax");
                throw new RuntimeException("invalid syntax");
            }
            StringBuilder sql = new StringBuilder();
            sql.append("update ").append(holder.getTable()).append("\n").append(" set ")
                    .append(Joiner.on(",")
                            .join(values.values().stream().sorted(Comparator.comparing(Expr::getVName))
                                    .map(Expr::getExpr).collect(Collectors.toList())))
                    .append("\n").append(params.getSqlWhere());
            return SqlParams.model(holder, sql, Lists.newArrayList(expr(expr(Maps.newHashMap(), params), values)));
        }

        public static class MysqlBuilder extends SqlBuilder {

            private Expr newExpr(Property p, Type type, boolean upsert, Object value) {
                String vname = p.getName() + "#" + type.name() + NEW_VALUE_SUFFIX;
                String expr;
                switch ((VType) type) {
                    case EQ:
                        expr = p.getColumn() + " = :" + vname;
                        break;
                    case ADD:
                    case SUB:
                        if (upsert) {
                            expr = p.getColumn() + " = values(" + p.getColumn() + ") " + type.symbol() + " :" + vname;
                        } else {
                            expr = p.getColumn() + " = " + p.getColumn() + " " + type.symbol() + " :" + vname;
                        }
                        break;
                    case EXPR:
                        expr = p.getColumn() + " = " + value;
                        break;
                    default:
                        throw new RuntimeException("not support");
                }
                return new Expr(p, type, vname, expr, value);
            }

            @Override
            public Map<String, Expr> valueExprs(Model model, boolean upsert, Map<String, Object> values) {
                Map<String, Expr> result = Maps.newHashMap();
                if (!CollectionUtils.isEmpty(values)) {
                    values.forEach((key, value) -> {
                        String[] s = key.split("#");
                        s[0] = s[0].trim();
                        Property p = model.getProperty(s[0]);
                        SqlBuilder.VType op = SqlBuilder.VType.EQ;
                        if (s.length == 2) {
                            switch (s[1].trim().toUpperCase()) {
                                case "EXPR":
                                    op = SqlBuilder.VType.EXPR;
                                    break;
                                case "+":
                                case "ADD":
                                    op = SqlBuilder.VType.ADD;
                                    break;
                                case "-":
                                case "SUB":
                                    op = SqlBuilder.VType.SUB;
                                    break;
                                default:
                                    break;
                            }
                        }
                        result.putIfAbsent(p.getName(), newExpr(p, op, upsert, value));
                    });
                }
                model.getUpdateTimeProperties().forEach(
                        p -> result.putIfAbsent(p.getName(), newExpr(p, VType.EQ, upsert, p.getUpdateDef().get())));
                return result;
            }

            @Override
            public <T> SqlParams insert(Model model, ShardHolder holder, List<T> objs, boolean ignore) {
                StringBuilder sql = new StringBuilder();
                sql.append("insert");
                if (ignore) {
                    sql.append(" ignore	 ");
                }
                sql.append(" into ").append(holder.getTable()).append("\n").append(" (")
                        .append(Joiner.on(",")
                                .join(model.getInsertProperties().stream().map(Property::getColumn)
                                        .collect(Collectors.toList())))
                        .append(") ").append("\n").append("values").append("\n")
                        .append(Joiner.on(",\n")
                                .join(IntStream.range(0, objs.size()).boxed().map(i -> "("
                                        + Joiner.on(",")
                                        .join(model.getInsertProperties().stream()
                                                .map(p -> ":" + p.getName() + i).collect(Collectors.toList()))
                                        + ")").collect(Collectors.toList())));
                Map<String, Object> params = Maps.newHashMap();
                IntStream.range(0, objs.size()).boxed().forEach(i -> model.getInsertProperties()
                        .forEach(p -> params.put(p.getName() + i, p.getOrInsertDef(objs.get(i)))));
                return SqlParams.model(holder, sql, Lists.newArrayList(params));
            }

            @Override
            public <T> SqlParams upsert(Model model, ShardHolder holder, List<T> objs, Map<String, Expr> values) {
                if (CollectionUtils.isEmpty(values)) {
                    return this.insert(model, holder, objs, true);
                }
                SqlParams sqlParams = this.insert(model, holder, objs, false);
                sqlParams.getSql().append(" on duplicate key update ").append(
                        Joiner.on(",").join(values.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
                                .map(e -> e.getValue().getExpr()).collect(Collectors.toList())));
                expr(sqlParams.getParamsList().get(0), values);
                return sqlParams;
            }

            @Override
            public SqlParams select(Model model, ShardHolder holder, Collection<String> columns,
                                    ParamsBuilder.Params params, List<String> groups, List<String> orders, Integer offset,
                                    Integer limit) {
                StringBuilder sql = new StringBuilder();
                sql.append("select ");
                if (CollectionUtils.isEmpty(columns)) {
                    sql.append("*");
                } else {
                    sql.append(Joiner.on(",").join(columns.stream().map(String::trim).sorted().map(n -> {
                        Property property = model.getProperty(n);
                        if (property == null) {
                            return n;
                        }
                        return property.getColumn();
                    }).collect(Collectors.toList())));
                }
                sql.append(" from ").append(holder.getTable()).append(params.getSqlWhere());
                if (!CollectionUtils.isEmpty(groups)) {
                    sql.append(" group by ").append(Joiner.on(",").join(groups.stream().map(String::trim).sorted()
                            .map(g -> model.getProperty(g).getColumn()).collect(Collectors.toList())));
                }
                if (!CollectionUtils.isEmpty(orders)) {
                    sql.append(" order by ")
                            .append(Joiner.on(",").join(orders.stream().map(String::trim).sorted().map(o -> {
                                String[] s = o.split("#");
                                s[0] = s[0].trim();
                                Property p = model.getProperty(s[0]);
                                String t = " ASC ";
                                if (s.length == 2 && s[1].toUpperCase().equals("DESC")) {
                                    t = " DESC ";
                                }
                                if (p == null) {
                                    return s[0] + t;
                                }
                                return p.getColumn() + t;
                            }).collect(Collectors.toList())));
                }
                Map<String, Object> paramMap = expr(Maps.newHashMap(), params);
                if (offset != null) {
                    paramMap.put(VAR_OFFSET, offset);
                    paramMap.put(VAR_LIMIT, limit == null ? 1 : limit);
                    sql.append(" limit :").append(VAR_OFFSET).append(",:").append(VAR_LIMIT);
                } else if (limit != null) {
                    paramMap.put(VAR_LIMIT, limit);
                    sql.append(" limit :").append(VAR_LIMIT);
                }
                return SqlParams.model(holder, sql, Lists.newArrayList(paramMap));
            }
        }
    }

}
