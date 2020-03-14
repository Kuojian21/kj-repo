package com.kj.repo.infra.compress.lz4;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.kj.repo.infra.compress.Compress;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4SafeDecompressor;

/**
 * @author kj
 * Created on 2020-03-14
 */
public abstract class Lz4Compress implements Compress {
    protected final LZ4Factory factory;

    public Lz4Compress(LZ4Factory factory) {
        this.factory = factory;
    }

    @Override
    public byte[] compress(byte[] src) {
        LZ4Compressor compressor = factory.fastCompressor();
        return compressor.compress(src);
    }

    @Override
    public byte[] decompress(byte[] src) {
        LZ4FastDecompressor decompressor = factory.fastDecompressor();
        return decompressor.decompress(src, src.length);
    }

    @Override
    public OutputStream compress(OutputStream out) throws IOException {
        LZ4Compressor compressor = factory.fastCompressor();
        return new LZ4BlockOutputStream(out, 1024 * 1024, compressor);
    }

    @Override
    public InputStream decompress(InputStream is) throws IOException {
        LZ4FastDecompressor decompressor = factory.fastDecompressor();
        return new LZ4BlockInputStream(is, decompressor);
    }

    public LZ4Factory getFactory() {
        return factory;
    }


    public byte[] decompressSafe(byte[] dest) {
        LZ4SafeDecompressor decompressor = factory.safeDecompressor();
        return decompressor.decompress(dest, dest.length);
    }


    public LZ4FrameInputStream decompressSafe(InputStream is) throws IOException {
        LZ4SafeDecompressor decompressor = factory.safeDecompressor();
        return new LZ4FrameInputStream(is);
    }
}
