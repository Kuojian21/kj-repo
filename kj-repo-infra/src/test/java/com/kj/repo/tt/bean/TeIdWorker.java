package com.kj.repo.tt.bean;

import java.util.concurrent.CountDownLatch;

import com.kj.repo.infra.bean.BeanIDWorker;

public class TeIdWorker {
    public static void main(String[] args) throws InterruptedException {
        BeanIDWorker worker = new BeanIDWorker(1, 1);
        CountDownLatch latch = new CountDownLatch(100);
        for (int i = 0; i < 100; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        System.out.println(Long.toHexString(worker.getId()));
                    }
                }
            }).start();
        }
        latch.await();
    }
}
