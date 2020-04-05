package com.kj.repo.tt.bean;

import java.util.concurrent.CountDownLatch;

import com.kj.repo.infra.algorithm.Snowflake;

public class TeIdWorker {
    public static void main(String[] args) throws InterruptedException {
        Snowflake worker = new Snowflake(1, 1);
        CountDownLatch latch = new CountDownLatch(100);
        for (int i = 0; i < 100; i++) {
            new Thread(() -> {
                latch.countDown();
                while (true) {
                    System.out.println(worker.nextId());
                }
            }).start();
        }
        latch.await();
    }
}
