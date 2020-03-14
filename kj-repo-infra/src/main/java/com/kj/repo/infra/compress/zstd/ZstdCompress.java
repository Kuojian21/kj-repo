package com.kj.repo.infra.compress.zstd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdDictCompress;
import com.github.luben.zstd.ZstdDictDecompress;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.kj.repo.infra.compress.Compress;

/**
 * @author kj
 * Created on 2020-03-14
 */
public class ZstdCompress implements Compress {
    private final byte[] dict;
    private final int dictlength;
    private final ZstdDictCompress zstdDictCompress;
    private final ZstdDictDecompress zstdDictDecompress;

    public ZstdCompress(byte[] dict, int dictlength, int level) {
        this.dict = dict;
        this.dictlength = dictlength;
        this.zstdDictCompress = new ZstdDictCompress(dict, 0, dictlength, level);
        this.zstdDictDecompress = new ZstdDictDecompress(dict, 0, dict.length);
    }

    @Override
    public byte[] compress(byte[] src) {
        return Zstd.compress(ByteBuffer.wrap(src), zstdDictCompress).array();
    }

    @Override
    public byte[] decompress(byte[] src) {
        return Zstd.decompress(src, zstdDictDecompress, src.length);
    }

    @Override
    public ZstdOutputStream compress(OutputStream out) throws IOException {
        ZstdOutputStream zos = new ZstdOutputStream(out);
        zos.setDict(zstdDictCompress);
        return zos;
    }

    @Override
    public ZstdInputStream decompress(InputStream is) throws IOException {
        ZstdInputStream zis = new ZstdInputStream(is);
        zis.setDict(zstdDictDecompress);
        return zis;
    }
}
