package com.kj.repo.infra.perf;

import com.kj.repo.infra.perf.model.PerfBuilder;
import com.kj.repo.infra.perf.model.PerfLog;

/**
 * @author kj
 */
public class PerfHelper {

    public static PerfBuilder perf(String namespace, String tag, Object... extras) {
        return new PerfBuilder(PerfLog.builder().setNamespace(namespace).setTag(tag).addExtras(extras).build());
    }

    public static void logstash(PerfBuilder builder) {
        PerfLogger.DEFAULT.logstash(builder);
    }

}
