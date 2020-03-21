package com.kj.repo.infra.savor.sql;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kj.repo.infra.logger.LoggerHelper;
import com.kj.repo.infra.savor.Savor;
import com.kj.repo.infra.savor.model.Expr;
import com.kj.repo.infra.savor.model.IModel;
import com.kj.repo.infra.savor.model.ShardHolder;
import com.kj.repo.infra.savor.model.SqlParams;

/**
 * @author kj
 */
public abstract class SqlBuilder {

    private static final String NEW_VALUE_SUFFIX = "$newValueSuffix$";
    private static final String VAR_LIMIT = "$limit$";
    private static final String VAR_OFFSET = "$offset$";
    private static final ConcurrentMap<Savor.DBType, SqlBuilder> BUILDERS = Maps.newConcurrentMap();
    private static Logger logger = LoggerHelper.getLogger();

    static {
        register(Savor.DBType.MYSQL, new MysqlBuilder());
    }

    public static SqlBuilder sqlBuilder(Savor.DBType dbType) {
        return BUILDERS.get(dbType);
    }

    public static void register(Savor.DBType dbType, SqlBuilder sqlBuilder) {
        BUILDERS.putIfAbsent(dbType, sqlBuilder);
    }

    public static Map<String, Object> expr(Map<String, Object> paramMap, ParamsBuilder.Params params) {
        params.getMajor().forEach((key, value) -> value.forEach(v -> paramMap.put(v.getVname(), v.getValue())));
        params.getMinor().forEach((key, value) -> value.forEach(v -> paramMap.put(v.getVname(), v.getValue())));
        return paramMap;
    }

    public static Map<String, Object> expr(Map<String, Object> paramMap, Map<String, Expr> values) {
        values.values().forEach(v -> paramMap.put(v.getVname(), v.getValue()));
        return paramMap;
    }

    public abstract Map<String, Expr> valueExprs(IModel model, boolean upsert, Map<String, Object> values);

    public abstract <T> SqlParams insert(IModel model, ShardHolder holder, List<T> objs, boolean ignore);

    public abstract <T> SqlParams upsert(IModel model, ShardHolder holder, List<T> objs, Map<String, Expr> values);

    public abstract SqlParams select(IModel model, ShardHolder holder, Collection<String> columns,
            ParamsBuilder.Params params, List<String> groups, List<String> orders, Integer offset,
            Integer limit);

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
                .append(Joiner.on(",").join(values.values().stream().sorted(Comparator.comparing(Expr::getVname))
                        .map(Expr::getExpr).collect(Collectors.toList())))
                .append("\n").append(params.getSqlWhere());
        return SqlParams.model(holder, sql, Lists.newArrayList(expr(expr(Maps.newHashMap(), params), values)));
    }


    /**
     * @author kj
     */
    public static class MysqlBuilder extends SqlBuilder {

        private Expr newExpr(IModel.IProperty p, Expr.Type type, boolean upsert, Object value) {
            String vname = p.getName() + "#" + type.name() + NEW_VALUE_SUFFIX;
            String expr;
            switch ((Expr.VType) type) {
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
        public Map<String, Expr> valueExprs(IModel model, boolean upsert, Map<String, Object> values) {
            Map<String, Expr> result = Maps.newHashMap();
            if (!CollectionUtils.isEmpty(values)) {
                values.forEach((key, value) -> {
                    String[] s = key.split("#");
                    s[0] = s[0].trim();
                    IModel.IProperty p = model.getProperty(s[0]);
                    Expr.VType op = Expr.VType.EQ;
                    if (s.length == 2) {
                        switch (s[1].trim().toUpperCase()) {
                            case "EXPR":
                                op = Expr.VType.EXPR;
                                break;
                            case "+":
                            case "ADD":
                                op = Expr.VType.ADD;
                                break;
                            case "-":
                            case "SUB":
                                op = Expr.VType.SUB;
                                break;
                            default:
                                break;
                        }
                    }
                    result.putIfAbsent(p.getName(), newExpr(p, op, upsert, value));
                });
            }
            model.getUpdateDefProperties().forEach(
                    p -> result.putIfAbsent(p.getName(), newExpr(p, Expr.VType.EQ, upsert, p.getUpdateDef().get())));
            return result;
        }

        @Override
        public <T> SqlParams insert(IModel model, ShardHolder holder, List<T> objs, boolean ignore) {
            StringBuilder sql = new StringBuilder();
            sql.append("insert");
            if (ignore) {
                sql.append(" ignore ");
            }
            sql.append(" into ").append(holder.getTable()).append("\n").append(" (")
                    .append(Joiner.on(",").join(
                            model.getInsertProperties().stream().map(IModel.IProperty::getColumn)
                                    .collect(Collectors.toList())))
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
            return SqlParams.model(holder, sql, Lists.newArrayList(params));
        }

        @Override
        public <T> SqlParams upsert(IModel model, ShardHolder holder, List<T> objs, Map<String, Expr> values) {
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
        public SqlParams select(IModel model, ShardHolder holder, Collection<String> columns,
                ParamsBuilder.Params params, List<String> groups, List<String> orders, Integer offset,
                Integer limit) {
            StringBuilder sql = new StringBuilder();
            sql.append("select ");
            if (CollectionUtils.isEmpty(columns)) {
                sql.append("*");
            } else {
                sql.append(Joiner.on(",").join(columns.stream().map(String::trim).sorted().map(n -> {
                    IModel.IProperty property = model.getProperty(n);
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
                            IModel.IProperty p = model.getProperty(s[0]);
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