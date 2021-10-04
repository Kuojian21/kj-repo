package com.kj.repo.demo.algorithm;

import java.util.concurrent.CountDownLatch;

public class TeSnowflake {
    public static void main(String[] args) throws InterruptedException {
        Snowflake worker = new Snowflake(1, 1);
        CountDownLatch latch = new CountDownLatch(100);
        for (int i = 0; i < 100; i++) {
            new Thread(() -> {
                latch.countDown();
                while (true) {
                    System.out.println(worker.next());
                }
            }).start();
        }
        latch.await();
    }
}
