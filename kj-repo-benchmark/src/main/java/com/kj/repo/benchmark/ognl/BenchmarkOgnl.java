package com.kj.repo.benchmark.ognl;

import java.lang.reflect.Member;
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
import com.kj.repo.infra.el.ognl.DefaultMemberAccess;
import com.kj.repo.infra.el.ognl.OgnlHelper;

import ognl.DefaultClassResolver;
import ognl.DefaultTypeConverter;
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
        bh.consume(OgnlHelper.execute(this, ImmutableMap.of("bean", new Bean()),
                "#bean.getVal() == 1 ? new com.kj.repo.benchmark.ognl.Bean1() : new com.kj.repo.benchmark.ognl"
                        + ".Bean2()", null));
    }
}
