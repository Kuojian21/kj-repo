package com.kj.repo.infra.perf;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.kj.repo.infra.batch.BatchBuffer;
import com.kj.repo.infra.batch.BatchTrigger;
import com.kj.repo.infra.perf.model.Perf;
import com.kj.repo.infra.perf.model.PerfBuilder;
import com.kj.repo.infra.perf.model.PerfLog;
import com.kj.repo.infra.perf.model.PerfStat;

/**
 * @author kj
 */
public abstract class PerfLogger {

    public static final PerfLogger DEFAULT = new PerfLogger() {

        private String header =
                format(Lists.newArrayList("name", "micro", "count", "maxMicro", "minMicro", "avg",
                        "variance", "top-95", "top-99"));

        @Override
        public void display(List<Map.Entry<PerfLog, Perf>> entries) {
            System.out.println(header);
            for (Map.Entry<PerfLog, Perf> entry : entries.stream().sorted(Comparator.comparing(e -> Joiner.on(".")
                    .join(e.getKey().getNamespace(), e.getKey().getTag(), Joiner.on(".").join(e.getKey().getExtras()))))
                    .collect(Collectors.toList())) {
                PerfLog perfLog = entry.getKey();
                List<Object> params = Lists.newArrayList();
                List<Object> names = Lists.newArrayList(perfLog.getNamespace(), perfLog.getTag());
                if (CollectionUtils.isNotEmpty(perfLog.getExtras())) {
                    names.addAll(perfLog.getExtras());
                }
                params.add(Joiner.on(".").join(names));
                Perf perf = entry.getValue();
                PerfStat perfStat = new PerfStat(perf);
                params.add(perf.getMicro());
                params.add(perf.getCount());
                params.add(perf.getMaxValue());
                params.add(perf.getMinValue());
                params.add(perfStat.getAvg());
                params.add(perfStat.getVariance());
                List<Double> percentiles = Lists.newArrayList(95D, 99D);
                Map<Double, Long> perMap = perfStat.getPercentiles(percentiles);
                params.addAll(percentiles.stream().map(perMap::get).collect(Collectors.toList()));
                System.out.println(format(params));
            }
            System.out.println();
        }

        private String format(List<Object> params) {
            return String
                    .format("%-30s %10s %10s %10s %10s %10s %10s %10s %10s", params.get(0).toString(),
                            params.get(1).toString(), params.get(2).toString(), params.get(3).toString(),
                            params.get(4).toString(), params.get(5).toString(), params.get(6).toString(),
                            params.get(7).toString(), params.get(8).toString());
        }
    };

    private final BatchTrigger<PerfBuilder> batchTrigger =
            BatchTrigger.<PerfBuilder, Map.Entry<PerfLog, Perf>> builder()
                    .setConsumer(this::display).setBuffer(
                    BatchBuffer.map(PerfBuilder::getPerfLog, e -> new Perf(e.getCount(), e.getMicro()),
                            (v1, v2) -> {
                                v1.accept(v2.getCount(), v2.getMicro());
                                return v1;
                            }))
                    .build();

    public void logstash(PerfBuilder builder) {
        batchTrigger.enqueue(builder);
    }

    protected abstract void display(List<Map.Entry<PerfLog, Perf>> entries);
}
