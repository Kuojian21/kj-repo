package com.kj.repo.infra.compress.lz4;

import java.io.IOException;
import java.io.OutputStream;

import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Compressor;

/**
 * @author kj
 * Created on 2020-03-14
 */
public class Lz4CompressDecoratorHigh extends Lz4CompressDecorator {
    private final int level;

    public Lz4CompressDecoratorHigh(Lz4Compress delegate) {
        super(delegate);
        level = -1;
    }

    public Lz4CompressDecoratorHigh(Lz4Compress delegate, int level) {
        super(delegate);
        this.level = level;
    }

    @Override
    public byte[] compress(byte[] src) {
        LZ4Compressor compressor;
        if (level < 0) {
            compressor = delegate.getFactory().highCompressor();
        } else {
            compressor = delegate.getFactory().highCompressor(level);
        }
        return compressor.compress(src);
    }

    @Override
    public OutputStream compress(OutputStream out) throws IOException {
        LZ4Compressor compressor;
        if (level < 0) {
            compressor = delegate.getFactory().highCompressor();
        } else {
            compressor = delegate.getFactory().highCompressor(level);
        }
        return new LZ4BlockOutputStream(out, 1024 * 1024, compressor);
    }
}
