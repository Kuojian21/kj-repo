package com.kj.repo.infra.perf.model;

import java.util.List;
import java.util.Objects;

import com.google.common.collect.Lists;

/**
 * @author kj
 */
public class PerfLog {

    private final String namespace;
    private final String tag;
    private final List<Object> extras;

    public PerfLog(String namespace, String tag, List<Object> extras) {
        this.namespace = namespace;
        this.tag = tag;
        this.extras = extras;
    }

    public static PerfLogBuilder builder() {
        return new PerfLogBuilder();
    }

    public String getNamespace() {
        return namespace;
    }

    public String getTag() {
        return tag;
    }

    public List<Object> getExtras() {
        return extras;
    }

    @Override
    public int hashCode() {
        List<Object> values = Lists.newArrayList();
        values.add(namespace);
        values.add(tag);
        values.addAll(extras);
        return Objects.hash(values.toArray());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        }
        PerfLog other = (PerfLog) obj;
        if (!nullToEmpty(namespace).equals(nullToEmpty(other.namespace))
                || !nullToEmpty(tag).equals(nullToEmpty(other.tag))) {
            return false;
        } else {
            List<Object> ex1 = nullToEmpty(extras);
            List<Object> ex2 = nullToEmpty(other.extras);
            if (ex1.size() != ex2.size()) {
                return false;
            }
            for (int i = 0, len = ex1.size(); i < len; i++) {
                if (!nullToEmpty(ex1.get(i)).equals(nullToEmpty(ex2.get(i)))) {
                    return false;
                }
            }
            return true;
        }

    }

    public Object nullToEmpty(Object obj) {
        return obj == null ? "" : obj;
    }

    public List<Object> nullToEmpty(List<Object> list) {
        return list == null ? Lists.newArrayList() : list;
    }

}
