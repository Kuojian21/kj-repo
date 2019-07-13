package com.kj.repo.infra.savor.sql;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kj.repo.infra.savor.model.Expr;
import com.kj.repo.infra.savor.model.IModel;

/**
 * @author kuojian21
 */
public class ParamsBuilder {

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

    public Params build(IModel model, String suffix) {
        Map<String, List<Expr>> result = Maps.newHashMap();
        int[] index = new int[]{0};
        if (!CollectionUtils.isEmpty(this.major)) {
            this.major.forEach((key, value) -> {
                String[] s = key.split("#");
                s[0] = s[0].trim();
                IModel.IProperty p = model.getProperty(s[0]);
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
                    if (op == Expr.PType.EQ) {
                        op = Expr.PType.IN;
                    } else if (op == Expr.PType.NE) {
                        op = Expr.PType.NOTIN;
                    } else {
                        throw new RuntimeException("invalid syntax:" + key);
                    }
                    if (value.getClass().isArray()) {
                        result.computeIfAbsent(p.getName(), k -> Lists.newArrayList())
                                .add(Expr.param(p, op, Arrays.asList((Object[]) value), suffix + "#" + index[0]++));
                    } else {
                        result.computeIfAbsent(p.getName(), k -> Lists.newArrayList())
                                .add(Expr.param(p, op, value, suffix + "#" + index[0]++));
                    }
                } else {
                    result.computeIfAbsent(p.getName(), k -> Lists.newArrayList())
                            .add(Expr.param(p, op, value, suffix + "#" + index[0]++));
                }
            });
        }
        Params tParams = new Params(this.conn, result);
        List<String> exprs = tParams.getMajor().entrySet().stream().flatMap(e -> e.getValue().stream())
                .sorted(Comparator.comparing(Expr::getVname)).map(Expr::getExpr).collect(Collectors.toList());
        this.minor.stream().map(pb -> pb.build(model, suffix + "#" + index[0]++))
                .sorted(Comparator.comparing(p -> p.getWhere().toString())).forEach(p -> {
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

        public Params(CONN conn, StringBuilder where, Map<String, List<Expr>> major, Map<String, List<Expr>> minor) {
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

        public CONN getConn() {
            return conn;
        }

        public StringBuilder getWhere() {
            return where;
        }

        public Map<String, List<Expr>> getMajor() {
            return major;
        }

        public Map<String, List<Expr>> getMinor() {
            return minor;
        }
    }
}