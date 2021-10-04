package com.kj.repo.tt.perf;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.Maps;
import com.kj.repo.infra.utils.RunUtil;

/**
 * @author kj
 */
public class TePerf {

    public static void main(String[] args) {
        ConcurrentMap<Integer, Integer> map = Maps.newConcurrentMap();
        AtomicLong count = new AtomicLong(0);
        CountDownLatch latch = new CountDownLatch(1);
        for (int j = 0; j < 200; j++) {
            new Thread(() -> RunUtil.run(() -> {
                latch.await();
                for (int i = 0; i < Integer.MAX_VALUE; i++) {
                    RunUtil.run(() -> {
                        count.addAndGet(map.entrySet().stream().limit(10).count());
                        Thread.sleep(ThreadLocalRandom.current().nextLong(100));
                        return null;
                    }, "App", "app");
                }
                System.out.println(count.get());
                return null;
            })).start();
            new Thread(() -> RunUtil.run(() -> {
                latch.await();
                for (int i = 0; i < Integer.MAX_VALUE; i++) {
                    int t = i;
                    RunUtil.run(() -> {
                        map.put(t, t);
                        Thread.sleep(ThreadLocalRandom.current().nextLong(100));
                        return null;
                    }, "App", "app");
                }
                return null;
            })).start();
        }
        latch.countDown();
        System.out.println("App,app");
    }
}
