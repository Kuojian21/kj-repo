package com.kj.repo.infra.utils.compress;

import net.jpountz.lz4.LZ4Factory;

/**
 * @author kj
 * Created on 2020-03-14
 */
public class Lz4CompressSafe extends Lz4Compress {
    public Lz4CompressSafe() {
        super(LZ4Factory.safeInstance());
    }
}
