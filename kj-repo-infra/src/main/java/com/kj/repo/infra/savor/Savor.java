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
import org.springframework.util.CollectionUtils;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
        this.model = ModelHelper.model(this.clazz);
    }

    protected Savor(Class<T> clazz) {
        this.clazz = clazz;
        this.rowMapper = new BeanPropertyRowMapper<>(this.clazz);
        this.model = ModelHelper.model(this.clazz);
    }

    public int update(SqlParams sqlParams) {
        logger.info("sql:{}", sqlParams.getSql());
        return sqlParams.shardHolder.jdbcTemplateHolder.writer.update(sqlParams.getSql().toString(), sqlParams.getParams());
    }

    public int update(Stream<SqlParams> stream) {
        return stream.map(this::update).mapToInt(r -> r).sum();
    }

    public <R> List<R> select(SqlParams sqlParams, RowMapper<R> rowMapper) {
        logger.info("sql:{}", sqlParams.getSql());
        return sqlParams.shardHolder.jdbcTemplateHolder.reader.query(sqlParams.getSql().toString(), sqlParams.getParams(), rowMapper);
    }

    public <R> List<R> select(Stream<SqlParams> stream, RowMapper<R> rowMapper) {
        return stream.map(s -> this.select(s, rowMapper)).flatMap(Collection::stream).collect(Collectors.toList());
    }

    public int insert(List<T> objs) {
        return this.insert(objs, true);
    }

    public int insert(List<T> objs, boolean ignore) {
        if (objs == null || objs.isEmpty()) {
            return 0;
        }
        return this.update(this.shard(objs).entrySet().stream()
                .map(e -> SqlHelper.insert(this.model, e.getKey(), e.getValue(), ignore)));
    }

    public int upsert(List<T> objs, Collection<String> names) {
        if (objs == null || objs.isEmpty()) {
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
        return this.upsert(objs, ValuesBuilder.of(true).with(values));
    }

    public int upsert(List<T> objs, ValuesBuilder valuesBuilder) {
        if (objs == null || objs.isEmpty()) {
            return 0;
        }
        return this.update(this.shard(objs).entrySet().stream()
                .map(e -> SqlHelper.upsert(this.model, e.getKey(), e.getValue(), valuesBuilder.build(this.model))));
    }

    public int delete(Map<String, Object> params) {
        return this.delete(ParamsBuilder.ofAnd().with(params));
    }

    public int delete(ParamsBuilder paramsBuilder) {
        return this.update(this.shard(paramsBuilder).entrySet().stream()
                .map(e -> SqlHelper.delete(e.getKey(), e.getValue())));
    }

    public int update(Map<String, Object> values, Map<String, Object> params) {
        return this.update(ValuesBuilder.of(false).with(values), ParamsBuilder.ofAnd().with(params));
    }

    public int update(ValuesBuilder valuesBuilder, ParamsBuilder paramsBuilder) {
        return this.update(this.shard(paramsBuilder).entrySet().stream()
                .map(e -> SqlHelper.update(e.getKey(), valuesBuilder.build(this.model), e.getValue())));
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
        return this.select(this.shard(paramsBuilder).entrySet().stream().map(
                e -> SqlHelper.select(this.model, e.getKey(), columns, e.getValue(), groups, orders, offset, limit)),
                rowMapper);
    }

    protected Map<ShardHolder, ParamsBuilder.Params> shard(ParamsBuilder paramsBuilder) {
        ParamsBuilder.Params params = paramsBuilder.build(this.model);
        Map<String, List<Param>> tParams = params.master;
        Property property = this.model.getShardProperty();
        if (property == null) {
            return Helper.newHashMap(
                    new ShardHolder(new JdbcTemplateHolder(this.getReader(), this.getWriter()), this.model.getTable()), params);
        }
        List<Param> paramList = tParams.get(property.getName());
        if (paramList == null || params.getConn() == ParamsBuilder.CONN.OR) {
            ShardHolder holder = this.shardWrap().apply(null);
            if (holder != null) {
                return Helper.newHashMap(holder, params);
            }
            return this.shards().stream().collect(Collectors.toMap(t -> t, t -> params));
        } else if (paramList.size() == 1) {
            Param param = paramList.get(0);
            switch (param.op) {
                case EQ:
                    return Helper.newHashMap(this.shardWrap().apply(param.getValue()), params);
                case IN:
                    return ((Collection<?>) param.getValue()).stream()
                            .map(e -> Tuple.tuple(this.shardWrap().apply(e), e))
                            .collect(Collectors.groupingBy(Tuple::getX)).entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey,
                                    e -> params.copy(property.getName(), Lists.newArrayList(new Param(property, Param.OP.IN,
                                            e.getValue().stream().map(Tuple::getY).collect(Collectors.toList()))))));
                default:
                    return this.shards().stream().collect(Collectors.toMap(t -> t, t -> params));
            }
        } else {
            return this.shards().stream().collect(Collectors.toMap(t -> t, t -> params));
        }

    }

    protected Map<ShardHolder, List<T>> shard(List<T> objs) {
        Property property = this.model.getShardProperty();
        if (property == null) {
            return Helper.newHashMap(new ShardHolder(new JdbcTemplateHolder(this.getReader(), this.getWriter()), this.model.getTable()), objs);
        } else {
            return objs.stream()
                    .collect(Collectors.groupingBy(o -> this.shardWrap().apply(property.getOrInsertDef(o))));
        }
    }

    public Model getModel() {
        return this.model;
    }

    public Class<T> getClazz() {
        return this.clazz;
    }

    public RowMapper<T> getRowMapper() {
        return this.rowMapper;
    }

    protected Function<Object, ShardHolder> shard() {
        throw new RuntimeException("not supported");
    }

    protected List<ShardHolder> shards() {
        throw new RuntimeException("not supported");
    }

    protected Function<Object, ShardHolder> shardWrap() {
        return (key) -> this.shard().apply(this.model.shardProperty.cast(key));
    }

    public abstract NamedParameterJdbcTemplate getReader();

    public abstract NamedParameterJdbcTemplate getWriter();

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
    public static class ModelHelper {

        private static final ConcurrentMap<Class<?>, Model> MODELS = Maps.newConcurrentMap();

        public static Model model(Class<?> clazz) {
            return MODELS.computeIfAbsent(clazz, Model::new);
        }

        static Supplier<Object> insertDef(Field f) {
            TimeInsert inDef = f.getAnnotation(TimeInsert.class);
            if (inDef != null) {
                return parseDef(f.getType(), inDef.value());
            } else {
                return () -> null;
            }
        }

        static Supplier<Object> updateDef(Field f) {
            TimeUpdate upDef = f.getAnnotation(TimeUpdate.class);
            if (upDef != null) {
                return parseDef(f.getType(), upDef.value());
            } else {
                return () -> null;
            }
        }

        static Supplier<Object> parseDef(Class<?> type, String value) {
            if (Long.class.equals(type)) {
                return System::currentTimeMillis;
            } else if (java.sql.Date.class.equals(type)) {
                return () -> new java.sql.Date(System.currentTimeMillis());
            } else if (Timestamp.class.equals(type)) {
                return () -> new Timestamp(System.currentTimeMillis());
            }
            return () -> null;
        }

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

        public static Map<String, Object> paramMap(ParamsBuilder.Params params) {
            Map<String, Object> paramMap = Maps.newHashMap();
            params.master.forEach((key, value) -> value.forEach(v -> paramMap.put(v.getVName(), v.getValue())));
            params.others.forEach((key, value) -> value.forEach(v -> paramMap.put(v.getVName(), v.getValue())));
            return paramMap;
        }

        public static Map<String, Object> valueMap(Map<String, Object> paramMap, Map<String, Value> values) {
            values.values().forEach(v -> paramMap.put(v.getVName(), v.getValue()));
            return paramMap;
        }
    }

    /**
     * @author kuojian21
     */
    public static class SqlHelper {

        private static final String VAR_LIMIT = "$limit$";
        private static final String VAR_OFFSET = "$offset$";

        public static <T> SqlParams insert(Model model, ShardHolder holder, List<T> objs, boolean ignore) {
            StringBuilder sql = new StringBuilder();
            sql.append("insert");
            if (ignore) {
                sql.append(" ignore	 ");
            }
            sql.append(" into ").append(holder.getTable()).append("\n").append(" (")
                    .append(Joiner.on(",").join(
                            model.getInsertProperties().stream().map(Property::getColumn).collect(Collectors.toList())))
                    .append(") ").append("\n").append("values").append("\n")
                    .append(Joiner.on(",\n").join(IntStream
                            .range(0, objs.size()).boxed().map(
                                    i -> "(" + Joiner.on(",")
                                            .join(model.getInsertProperties().stream().map(p -> ":" + p.getName() + i)
                                                    .collect(Collectors.toList()))
                                            + ")")
                            .collect(Collectors.toList())));
            Map<String, Object> params = Maps.newHashMap();
            IntStream.range(0, objs.size()).boxed().forEach(i -> model.getInsertProperties()
                    .forEach(p -> params.put(p.getName() + i, p.getOrInsertDef(objs.get(i)))));
            return SqlParams.model(holder, sql, params);
        }

        public static <T> SqlParams upsert(Model model, ShardHolder holder, List<T> objs, ValuesBuilder.Values values) {
            if (CollectionUtils.isEmpty(values.getValues())) {
                return SqlHelper.insert(model, holder, objs, true);
            }
            SqlParams sqlParams = SqlHelper.insert(model, holder, objs, false);
            sqlParams.getSql().append(" on duplicate key update ")
                    .append(Joiner.on(",")
                            .join(values.values.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
                                    .map(e -> e.getValue().getExpr()).collect(Collectors.toList())));
            Helper.valueMap(sqlParams.getParams(), values.getValues());
            return sqlParams;
        }

        public static SqlParams delete(ShardHolder holder, ParamsBuilder.Params params) {
            StringBuilder sql = new StringBuilder();
            sql.append("delete from ").append(holder.getTable()).append("\n").append(params.getWhere(true));
            return SqlParams.model(holder, sql, Helper.paramMap(params));
        }

        public static SqlParams update(ShardHolder holder, ValuesBuilder.Values values, ParamsBuilder.Params params) {
            if (CollectionUtils.isEmpty(values.getValues())) {
                logger.error("invalid syntax");
                throw new RuntimeException("invalid syntax");
            }
            StringBuilder sql = new StringBuilder();
            sql.append("update ").append(holder.getTable()).append("\n").append(" set ")
                    .append(Joiner.on(",")
                            .join(values.getValues().values().stream().sorted(Comparator.comparing(Value::getVName))
                                    .map(Value::getExpr).collect(Collectors.toList())))
                    .append("\n").append(params.getWhere(true));
            return SqlParams.model(holder, sql, Helper.valueMap(Helper.paramMap(params), values.getValues()));
        }

        public static SqlParams select(Model model, ShardHolder holder, Collection<String> columns,
                                       ParamsBuilder.Params params, List<String> groups, List<String> orders, Integer offset, Integer limit) {
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
            sql.append(" from ").append(holder.getTable()).append(params.getWhere(true));
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
            Map<String, Object> paramMap = Helper.paramMap(params);
            if (offset != null) {
                paramMap.put(VAR_OFFSET, offset);
                paramMap.put(VAR_LIMIT, limit == null ? 1 : limit);
                sql.append(" limit :").append(VAR_OFFSET).append(",:").append(VAR_LIMIT);
            } else if (limit != null) {
                paramMap.put(VAR_LIMIT, limit);
                sql.append(" limit :").append(VAR_LIMIT);
            }
            return SqlParams.model(holder, sql, paramMap);
        }

    }

    /**
     * @author kuojian21
     */
    @Getter
    public static class Model {
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
            Model pModel = Object.class.equals(clazz.getSuperclass()) ? null : ModelHelper.model(clazz.getSuperclass());
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
            this.insertDef = ModelHelper.insertDef(f);
            this.updateDef = ModelHelper.updateDef(f);
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
        private final Map<String, Object> params;

        public SqlParams(ShardHolder shardHolder, StringBuilder sql, Map<String, Object> params) {
            this.shardHolder = shardHolder;
            this.sql = sql;
            this.params = params;
        }

        public static SqlParams model(ShardHolder shardHolder, StringBuilder sql,
                                      Map<String, Object> params) {
            return new SqlParams(shardHolder, sql, params);
        }

    }

    /**
     * @author kuojian21
     */
    @Getter
    public static class ShardHolder {
        private final JdbcTemplateHolder jdbcTemplateHolder;
        private final String table;

        public ShardHolder(JdbcTemplateHolder jdbcTemplateHolder, String table) {
            this.jdbcTemplateHolder = jdbcTemplateHolder;
            this.table = table;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof ShardHolder) {
                ShardHolder o = (ShardHolder) other;
                return this.table.equals(o.table) /* && this.template.equals(o.template) */;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.table.hashCode() /* / 2 + this.template.hashCode() / 2 */;
        }

    }

    /**
     * @author kuojian21
     */
    @Getter
    public static class JdbcTemplateHolder {
        private final NamedParameterJdbcTemplate reader;
        private final NamedParameterJdbcTemplate writer;

        public JdbcTemplateHolder(NamedParameterJdbcTemplate reader, NamedParameterJdbcTemplate writer) {
            this.reader = reader;
            this.writer = writer;
        }
    }

    /**
     * @author kuojian21
     */
    @Getter
    public static class Expr {
        private final String vName;
        private final String expr;
        private final Object value;

        public Expr(String vName, String expr, Object value) {
            this.vName = vName;
            this.expr = expr;
            this.value = value;
        }
    }

    /**
     * @author kuojian21
     */
    public static class Param extends Expr {

        private final OP op;

        public Param(Property p, OP op, Object value) {
            super(getVName(p, op), getExpr(p, op), value);
            this.op = op;
        }

        public static String getVName(Property p, OP op) {
            return op == OP.EQ ? p.name : p.name + "#" + op.name();
        }

        public static String getExpr(Property p, OP op) {
            switch (op) {
                case IN:
                    return p.getColumn() + " in ( :" + getVName(p, op) + " )";
                case LT:
                case LE:
                case GT:
                case GE:
                case NE:
                case EQ:
                    return p.getColumn() + " " + op.symbol + " :" + getVName(p, op);
                default:
                    return "";
            }
        }

        /**
         * @author kuojian21
         */
        public enum OP {
            EQ("="), IN("in"), LT("<"), LE("<="), GT(">"), GE(">="), NE("!=");
            private final String symbol;

            OP(String symbol) {
                this.symbol = symbol;
            }

            public String getSymbol() {
                return symbol;
            }
        }
    }

    /**
     * @author kuojian21
     */
    @Getter
    public static class Value extends Expr {
        private static final String NEW_VALUE_SUFFIX = "$newValueSuffix$";
        private final OP op;

        public Value(Property p, OP op, boolean upsert, Object value) {
            super(getVName(p, op), getExpr(p, op, upsert, value), value);
            this.op = op;
        }

        public static String getVName(Property p, OP op) {
            return (op == OP.EQ ? p.name : p.name + "#" + op.name()) + NEW_VALUE_SUFFIX;
        }

        public static String getExpr(Property p, OP op, boolean upsert, Object value) {
            switch (op) {
                case EXPR:
                    return p.getColumn() + " = " + value;
                case ADD:
                case SUB:
                    if (upsert) {
                        return p.getColumn() + " = values(" + p.getColumn() + ") " + op.symbol + " :" + getVName(p, op);
                    }
                    return p.getColumn() + " = " + p.getColumn() + " " + op.symbol + " :" + getVName(p, op);
                case EQ:
                default:
                    return p.getColumn() + " = :" + getVName(p, op);
            }
        }

        /**
         * @author kuojian21
         */
        public enum OP {
            EQ("="), ADD("+"), SUB("-"), EXPR("EXPR");
            private final String symbol;

            OP(String symbol) {
                this.symbol = symbol;
            }

            public String getSymbol() {
                return symbol;
            }
        }
    }

    /**
     * @author kuojian21
     */
    public static class ParamsBuilder {

        private final CONN conn;
        private final Map<String, Object> master = Maps.newHashMap();
        private final List<ParamsBuilder> others = Lists.newArrayList();

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
            this.master.putAll(params);
            return this;
        }

        public ParamsBuilder with(String name, Object value) {
            this.master.put(name, value);
            return this;
        }

        public ParamsBuilder with(ParamsBuilder pBuilder) {
            this.others.add(pBuilder);
            return this;
        }

        public Params build(Model model) {
            Map<String, List<Param>> result = Maps.newHashMap();
            if (!CollectionUtils.isEmpty(this.master)) {
                this.master.forEach((key, value) -> {
                    String[] s = key.split("#");
                    s[0] = s[0].trim();
                    Property p = model.getProperty(s[0]);
                    Param.OP op = Param.OP.EQ;
                    if (s.length == 2) {
                        switch (s[1].trim().toUpperCase()) {
                            case "<=":
                            case "LE":
                                op = Param.OP.LE;
                                break;
                            case "<":
                            case "LT":
                                op = Param.OP.LT;
                                break;
                            case ">=":
                            case "GE":
                                op = Param.OP.GE;
                                break;
                            case ">":
                            case "GT":
                                op = Param.OP.GT;
                                break;
                            case "!=":
                            case "!":
                            case "<>":
                            case "NE":
                                op = Param.OP.NE;
                                break;
                            case "IN":
                            case "=":
                            case "EQ":
                                op = Param.OP.EQ;
                                break;
                            default:
                                logger.error("invalid syntax:{}", key);
                                throw new RuntimeException("invalid syntax:" + key);
                        }
                    }
                    if (value instanceof Collection || value.getClass().isArray()) {
                        if (op != Param.OP.EQ) {
                            logger.error("invalid syntax:{}", key);
                            throw new RuntimeException("invalid syntax:" + key);
                        } else if (value.getClass().isArray()) {
                            result.computeIfAbsent(p.getName(), k -> Lists.newArrayList())
                                    .add(new Param(p, Param.OP.IN, Arrays.asList((Object[]) value)));
                        } else {
                            result.computeIfAbsent(p.getName(), k -> Lists.newArrayList())
                                    .add(new Param(p, Param.OP.IN, value));
                        }
                    } else {
                        result.computeIfAbsent(p.getName(), k -> Lists.newArrayList()).add(new Param(p, op, value));
                    }
                });
            }
            Params tParams = new Params(this.conn, result);
            List<String> exprs = tParams.getMaster().entrySet().stream().flatMap(e -> e.getValue().stream())
                    .sorted(Comparator.comparing(Param::getVName)).map(Param::getExpr).collect(Collectors.toList());
            this.others.stream().map(pb -> pb.build(model)).sorted(Comparator.comparing(p -> p.getWhere().toString()))
                    .forEach(p -> {
                        exprs.add("(" + p.getWhere() + ")");
                        tParams.others.putAll(p.getMaster());
                        tParams.others.putAll(p.getOthers());
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
            private final Map<String, List<Param>> master;
            private final Map<String, List<Param>> others;

            public Params(ParamsBuilder.CONN conn, Map<String, List<Param>> master) {
                super();
                this.conn = conn;
                this.master = master;
                this.where = new StringBuilder();
                this.others = Maps.newHashMap();
            }

            public Params(CONN conn, StringBuilder where, Map<String, List<Param>> master,
                          Map<String, List<Param>> others) {
                this.conn = conn;
                this.where = where;
                this.master = master;
                this.others = others;
            }

            public Params copy(String name, List<Param> paramList) {
                Map<String, List<Param>> m = Maps.newHashMap(this.master);
                m.put(name, paramList);
                return new Params(this.conn, this.where, m, this.others);
            }

            public String getWhere(boolean includeWhere) {
                if (this.where.length() <= 0) {
                    return "";
                }
                if (includeWhere) {
                    return " where " + this.where.toString();
                }
                return this.where.toString();
            }

        }
    }

    /**
     * @author kuojian21
     */
    public static class ValuesBuilder {
        private final boolean upsert;
        private final Map<String, Object> values = Maps.newHashMap();

        public ValuesBuilder(boolean upsert) {
            this.upsert = upsert;
        }

        public static ValuesBuilder of(boolean upsert) {
            return new ValuesBuilder(upsert);
        }

        public ValuesBuilder with(Map<String, Object> values) {
            this.values.putAll(values);
            return this;
        }

        public ValuesBuilder with(String name, Object value) {
            this.values.put(name, value);
            return this;
        }

        public Values build(Model model) {
            Map<String, Value> result = Maps.newHashMap();
            if (!CollectionUtils.isEmpty(values)) {
                values.forEach((key, value) -> {
                    String[] s = key.split("#");
                    s[0] = s[0].trim();
                    Property p = model.getProperty(s[0]);
                    Value.OP op = Value.OP.EQ;
                    if (s.length == 2) {
                        switch (s[1].trim().toUpperCase()) {
                            case "EXPR":
                                op = Value.OP.EXPR;
                                break;
                            case "+":
                            case "ADD":
                                op = Value.OP.ADD;
                                break;
                            case "-":
                            case "SUB":
                                op = Value.OP.SUB;
                                break;
                            default:
                                break;
                        }
                    }
                    result.putIfAbsent(p.getName(), new Value(p, op, upsert, value));
                });
            }
            model.getUpdateTimeProperties().forEach(
                    p -> result.putIfAbsent(p.getName(), new Value(p, Value.OP.EQ, upsert, p.getUpdateDef().get())));
            return new Values(result);
        }

        /**
         * @author kuojian21
         */
        @Getter
        public static class Values {
            private final Map<String, Value> values;

            public Values(Map<String, Value> values) {
                this.values = values;
            }
        }
    }

}
