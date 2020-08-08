package com.kj.repo.benchmark.ognl;

import java.util.concurrent.ThreadLocalRandom;

public class Bean {
    private int val = ThreadLocalRandom.current().nextInt(2);

    public void bean() {

    }

    public int getVal() {
        return val;
    }
}