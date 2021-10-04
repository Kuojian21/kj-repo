package com.kj.repo.infra.dao.model;

/**
 * @author kj
 */
public class Expr {
    private final IModel.IProperty property;
    private final Type type;
    private final String vname;
    private final String expr;
    private final Object value;

    public Expr(IModel.IProperty property, Type type, String vname, String expr, Object value) {
        super();
        this.property = property;
        this.type = type;
        this.vname = vname;
        this.expr = expr;
        this.value = value;
    }

    public static Expr param(IModel.IProperty p, PType type, Object value, String suffix) {
        String vname = p.getName() + "#" + type.name() + suffix;
        String expr;
        switch (type) {
            case IN:
                expr = p.getColumn() + " in ( :" + vname + " )";
                break;
            case NOTIN:
                expr = p.getColumn() + " not in ( :" + vname + " )";
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

    public Expr copyWith(Object value) {
        return new Expr(this.property, this.type, this.vname, this.expr, value);
    }

    public IModel.IProperty getProperty() {
        return property;
    }

    public Type getType() {
        return type;
    }

    public String getVname() {
        return vname;
    }

    public String getExpr() {
        return expr;
    }

    public Object getValue() {
        return value;
    }

    /**
     * @author kj
     */
    public enum PType implements Type {
        EQ("="),
        IN("in"),
        LT("<"),
        LE("<="),
        GT(">"),
        GE(">="),
        NE("!="),
        NOTIN("not in");
        private final String symbol;

        PType(String symbol) {
            this.symbol = symbol;
        }

        public String symbol() {
            return symbol;
        }
    }

    /**
     * @author kj
     */
    public enum VType implements Expr.Type {
        EQ("="),
        ADD("+"),
        SUB("-"),
        EXPR("EXPR");
        private final String symbol;

        VType(String symbol) {
            this.symbol = symbol;
        }

        public String symbol() {
            return symbol;
        }
    }

    /**
     * @author kj
     */
    public interface Type {
        String name();

        String symbol();
    }
}