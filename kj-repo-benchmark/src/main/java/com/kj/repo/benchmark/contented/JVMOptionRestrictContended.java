package com.kj.repo.benchmark.contented;

import java.io.File;
import java.util.Collection;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;

import com.google.common.collect.Lists;

/**
 * @author kj
 * Created on 2020-03-22
 */
public class JVMOptionRestrictContended implements ExternalProfiler {

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        return Lists.newArrayList();
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params) {
        return Lists.newArrayList("-XX:-RestrictContended");
    }

    @Override
    public void beforeTrial(BenchmarkParams benchmarkParams) {

    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        return Lists.newArrayList();
    }

    @Override
    public boolean allowPrintOut() {
        return false;
    }

    @Override
    public boolean allowPrintErr() {
        return false;
    }

    @Override
    public String getDescription() {
        return this.getClass().getSimpleName();
    }
}
