package com.kj.repo.infra.compress.lz4;

import net.jpountz.lz4.LZ4Factory;

/**
 * @author kj
 * Created on 2020-03-14
 */
public class Lz4CompressNative extends Lz4Compress {
    public Lz4CompressNative() {
        super(LZ4Factory.nativeInstance());
    }
}
