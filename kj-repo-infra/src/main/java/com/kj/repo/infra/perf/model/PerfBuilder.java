package com.kj.repo.infra.perf.model;

import com.kj.repo.infra.perf.PerfHelper;

/**
 * @author kj
 */
public class PerfBuilder {

    private final PerfLog perfLog;
    private long count;
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

    public void setCount(long count) {
        this.count = count;
    }

    public long getMicro() {
        return micro;
    }

    public void setMicro(long micro) {
        this.micro = micro;
    }

    public void logstash() {
        PerfHelper.logstash(this);
    }
}
