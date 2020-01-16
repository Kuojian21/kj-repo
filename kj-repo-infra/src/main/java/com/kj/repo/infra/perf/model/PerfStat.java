package com.kj.repo.infra.perf.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.collect.Maps;

/**
 * @author kj
 */
public class PerfStat {
    private final Perf perf;
    private long avg;
    private long variance;
    private TreeMap<Long, Long> timeTreeMap;

    public PerfStat(Perf perf) {
        this.perf = perf;
        this.avg = perf.getMicro() / perf.getCount();

        this.timeTreeMap = new TreeMap<>(perf.getTimeMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue())));
        long truncatedSum = this.timeTreeMap.entrySet().stream().mapToLong(e -> e.getKey() * e.getValue()).sum();
        long truncatedAvg = truncatedSum / perf.getCount();
        this.timeTreeMap.forEach((number, count) -> {
            long diff = number - truncatedAvg;
            this.variance += diff * diff * count;
        });
        this.variance /= perf.getCount();
    }

    public Perf getPerf() {
        return perf;
    }

    public long getAvg() {
        return avg;
    }

    public long getVariance() {
        return variance;
    }

    public Map<Double, Long> getPercentiles(List<Double> percentiles) {
        if (CollectionUtils.isEmpty(percentiles)) {
            return Collections.emptyMap();
        }

        Map<Double, Long> result = Maps.newHashMapWithExpectedSize(percentiles.size());
        percentiles = new ArrayList<>(percentiles);
        percentiles.sort(Comparator.reverseOrder());
        int i = 0;
        double j = 0;
        for (Map.Entry<Long, Long> entry : this.timeTreeMap.descendingMap().entrySet()) {
            j += entry.getValue().doubleValue();
            double per = (1.0 - j / this.perf.getCount()) * 100;
            while (i < percentiles.size() && per < percentiles.get(i)) {
                result.put(percentiles.get(i), entry.getKey());
                i++;
            }
            if (i >= percentiles.size()) {
                break;
            }
        }
        return result;
    }

}
