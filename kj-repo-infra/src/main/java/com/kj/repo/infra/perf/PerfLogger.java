package com.kj.repo.infra.perf;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.kj.repo.infra.batch.BatchTrigger;
import com.kj.repo.infra.batch.buffer.Buffer;
import com.kj.repo.infra.perf.model.Perf;
import com.kj.repo.infra.perf.model.PerfBuilder;
import com.kj.repo.infra.perf.model.PerfLog;
import com.kj.repo.infra.perf.model.PerfStat;

/**
 * @author kj
 */
public abstract class PerfLogger {

    public static final PerfLogger DEFAULT = new PerfLogger() {
        @Override
        public void display(Entry<PerfLog, Perf> entry) {
            PerfLog perfLog = entry.getKey();
            List<Object> params = Lists.newArrayList(perfLog.getNamespace(), perfLog.getTag());
            if (CollectionUtils.isNotEmpty(perfLog.getExtras())) {
                params.add(Joiner.on(".").join(perfLog.getExtras()));
            }
            Perf perf = entry.getValue();
            PerfStat perfStat = new PerfStat(perf);
            params.add(perf.getSum());
            params.add(perf.getCount());
            params.add(perf.getMaxValue());
            params.add(perf.getMinValue());
            params.add(perfStat.getAvg());
            params.add(perfStat.getVariance());
            List<Double> percentiles = Lists.newArrayList(95D, 99D);
            Map<Double, Long> perMap = perfStat.getPercentiles(percentiles);
            params.addAll(percentiles.stream().map(perMap::get).collect(Collectors.toList()));
            System.out.println(Joiner.on(" ").join(params));
        }
    };

    private final BatchTrigger<PerfBuilder> batchTrigger = BatchTrigger.<PerfBuilder, Map.Entry<PerfLog, Perf>>builder()
            .setConsumer(this::display).setBuffer(Buffer.map(PerfBuilder::getPerfLog, e -> new Perf(),
                    (e, v) -> v.accept(e.getCount(), e.getMicro())))
            .build();

    public void logstash(PerfBuilder builder) {
        batchTrigger.enqueue(builder);
    }

    private void display(List<Map.Entry<PerfLog, Perf>> list) {
        list.forEach(this::display);
    }

    protected abstract void display(Map.Entry<PerfLog, Perf> entry);
}
