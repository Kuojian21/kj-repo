package com.kj.repo.infra.perf.model;

import java.util.List;

import com.google.common.collect.Lists;
/**
 * @author kj
 */
public class PerfLogBuilder {
    private String namespace;
    private String tag;
    private List<Object> extras = Lists.newArrayList();

    public PerfLogBuilder setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public PerfLogBuilder setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public PerfLogBuilder addExtras(Object... extras) {
        for (Object obj : extras) {
            this.extras.add(obj);
        }
        return this;
    }

    public PerfLog build() {
        return new PerfLog(namespace, tag, extras);
    }
}
