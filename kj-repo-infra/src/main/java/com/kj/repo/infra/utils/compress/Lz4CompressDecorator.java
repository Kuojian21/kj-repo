package com.kj.repo.infra.utils.compress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author kj
 * Created on 2020-03-14
 */
public abstract class Lz4CompressDecorator implements Compress {
    protected final Lz4Compress delegate;

    public Lz4CompressDecorator(Lz4Compress delegate) {
        this.delegate = delegate;
    }

    @Override
    public byte[] compress(byte[] src) {
        return delegate.compress(src);
    }

    @Override
    public byte[] decompress(byte[] src) {
        return delegate.decompress(src);
    }

    @Override
    public OutputStream compress(OutputStream out) throws IOException {
        return delegate.compress(out);
    }

    @Override
    public InputStream decompress(InputStream is) throws IOException {
        return delegate.decompress(is);
    }
}
