package com.kj.repo.benchmark.ognl;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import com.google.common.collect.ImmutableMap;
import com.kj.repo.infra.utils.el.OgnlUtil;

import ognl.DefaultClassResolver;
import ognl.DefaultTypeConverter;
import ognl.MemberAccess;
import ognl.Ognl;
import ognl.OgnlContext;

/**
 * @author kj
 * Created on 2020-08-04
 */
@BenchmarkMode({Mode.AverageTime})
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class BenchmarkOgnl {
    private static final DefaultMemberAccess MEMBER_ACCESS = new DefaultMemberAccess() {
        @Override
        public boolean isAccessible(Map context, Object target, Member member, String propertyName) {
            return true;
        }
    };
    private static final DefaultClassResolver CLASS_RESOLVER = new DefaultClassResolver();
    private static final DefaultTypeConverter TYPE_CONVERTER = new DefaultTypeConverter();
    private final Bean bean;
    private final String expr = "#bean.bean()";
    private final Object ognlExpr;
    private final OgnlContext ognlContext;
    public BenchmarkOgnl() {
        bean = new Bean();
        try {
            ognlExpr = Ognl.parseExpression(expr);
            ognlContext =
                    (OgnlContext) Ognl.createDefaultContext(this, MEMBER_ACCESS, CLASS_RESOLVER, TYPE_CONVERTER);
            ognlContext.put("bean", bean);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BenchmarkOgnl.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .warmupBatchSize(1)
                .warmupTime(TimeValue.seconds(30))
                .measurementIterations(1)
                .measurementBatchSize(1)
                .measurementTime(TimeValue.minutes(3))
                .threads(1)
                .timeout(TimeValue.seconds(30))
                .syncIterations(true)
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public void run1(Blackhole bh) {
        bh.consume(bean.getVal() == 1 ? new Bean1() : new Bean2());
    }
    //
    //    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    //    @Benchmark
    //    public void run2() throws OgnlException {
    //        Ognl.getValue(ognlExpr, ognlContext, ognlContext.getRoot());
    //    }
    //
    //    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    //    @Benchmark
    //    public void run3() throws OgnlException {
    //        Ognl.getValue(expr, ognlContext, ognlContext.getRoot());
    //    }

    @Benchmark
    public void run5(Blackhole bh) throws Exception {
        OgnlContext ognlContext =
                (OgnlContext) Ognl.createDefaultContext(this, MEMBER_ACCESS, CLASS_RESOLVER, TYPE_CONVERTER);
        ognlContext.put("bean", new Bean());
        bh.consume(Ognl.compileExpression(ognlContext, this,
                "#bean.getVal() == 1 ? new com.kj.repo.benchmark.ognl.Bean1() : new com.kj.repo.benchmark.ognl"
                        + ".Bean2()").getAccessor().get(ognlContext, ognlContext.getRoot()));
    }

    @Benchmark
    public void run6(Blackhole bh) throws Exception {
        bh.consume(OgnlUtil.execute(this, ImmutableMap.of("bean", new Bean()),
                "#bean.getVal() == 1 ? new com.kj.repo.benchmark.ognl.Bean1() : new com.kj.repo.benchmark.ognl"
                        + ".Bean2()", null));
    }
}

class DefaultMemberAccess implements MemberAccess {
    private boolean allowPrivate = true;
    private boolean allowProtected = true;
    private boolean allowDefault = true;

    public DefaultMemberAccess() {
        this(false, false, false);
    }

    public DefaultMemberAccess(boolean allowPrivate, boolean allowProtected, boolean allowDefault) {
        this.allowPrivate = allowPrivate;
        this.allowProtected = allowProtected;
        this.allowDefault = allowDefault;
    }

    @Override
    public Object setup(Map context, Object target, Member member, String propertyName) {
        Object result = null;
        if (isAccessible(context, target, member, propertyName)) {
            AccessibleObject accessible = (AccessibleObject) member;
            if (!accessible.isAccessible()) {
                result = Boolean.TRUE;
                accessible.setAccessible(true);
            }
        }
        return result;
    }

    @Override
    public void restore(Map context, Object target, Member member, String propertyName, Object state) {
        if (state != null) {
            ((AccessibleObject) member).setAccessible((Boolean) state);
        }
    }

    @Override
    public boolean isAccessible(Map context, Object target, Member member, String propertyName) {
        int modifiers = member.getModifiers();
        if (Modifier.isPublic(modifiers)) {
            return true;
        } else if (Modifier.isPrivate(modifiers)) {
            return allowPrivate;
        } else if (Modifier.isProtected(modifiers)) {
            return allowProtected;
        } else {
            return allowDefault;
        }
    }
}
