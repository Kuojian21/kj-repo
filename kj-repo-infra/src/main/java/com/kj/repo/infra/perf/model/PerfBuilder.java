package com.kj.repo.infra.perf.model;

import java.util.concurrent.TimeUnit;

import com.kj.repo.infra.perf.PerfHelper;

/**
 * @author kj
 */
public class PerfBuilder {

    private final PerfLog perfLog;
    private long count = 1;
    private long micro;

    public PerfBuilder(PerfLog perfLog) {
        super();
        this.perfLog = perfLog;
    }

    public PerfLog getPerfLog() {
        return perfLog;
    }

    public long getCount() {
        return count;
    }

    public PerfBuilder count(long count) {
        this.count = count;
        return this;
    }

    public long getMicro() {
        return micro;
    }

    public PerfBuilder millis(long millis) {
        this.micro = TimeUnit.MILLISECONDS.toMicros(millis);
        return this;
    }

    public PerfBuilder micro(long micro) {
        this.micro = micro;
        return this;
    }

    public void logstash() {
        PerfHelper.logstash(this);
    }
}
