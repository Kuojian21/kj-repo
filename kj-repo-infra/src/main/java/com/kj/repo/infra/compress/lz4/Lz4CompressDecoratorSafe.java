package com.kj.repo.infra.compress.lz4;

import java.io.IOException;
import java.io.InputStream;

import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4SafeDecompressor;
import net.jpountz.xxhash.XXHashFactory;

/**
 * @author kj
 * Created on 2020-03-14
 */
public class Lz4CompressDecoratorSafe extends Lz4CompressDecorator {
    public Lz4CompressDecoratorSafe(Lz4Compress delegate) {
        super(delegate);
    }

    @Override
    public byte[] compress(byte[] src) {
        LZ4SafeDecompressor decompressor = delegate.getFactory().safeDecompressor();
        return decompressor.decompress(src, src.length);
    }

    @Override
    public InputStream decompress(InputStream is) throws IOException {
        LZ4SafeDecompressor decompressor = delegate.getFactory().safeDecompressor();
        return new LZ4FrameInputStream(is, decompressor, XXHashFactory.fastestInstance().hash32());
    }
}
