package com.kj.repo.infra.algorithm;

import java.util.stream.IntStream;

public class Slide {

    private final int[] winds = new int[1000];
    private final long epoch = System.currentTimeMillis();
    private final long size = winds.length;
    private final int rate;

    private long index = 0L;
    private int count = 0;

    public Slide(int rate) {
        this.rate = rate;
        IntStream.range(0, (int) size).boxed().forEach(i -> winds[i] = 0);
    }

    public synchronized boolean tryAcquire() {
        long cIndex = System.currentTimeMillis() - epoch;
        if (cIndex - index + 1 > size) {
            IntStream.range(0, (int) size).boxed().forEach(i -> winds[i] = 0);
            count = 0;
            index = cIndex;
        } else {
            for (; this.index < cIndex; this.index++) {
                int t = (int) ((this.index + 1) % this.size);
                count -= winds[t];
                winds[t] = 0;
            }
        }

        if (this.count <= this.rate) {
            winds[(int) (index % this.size)] += 1;
            count++;
            return true;
        }
        return false;
    }

}
