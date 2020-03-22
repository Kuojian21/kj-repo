package com.kj.repo.benchmark.contented;

import sun.misc.Contended;

/**
 * @author kj
 * Created on 2020-03-22
 */
public class ContentedY {
    public volatile long a;
    @Contended
    public volatile long b;
    @Contended
    public volatile long c;
    @Contended
    public volatile long d;
    @Contended
    public volatile long e;
    @Contended
    public volatile long f;
}
