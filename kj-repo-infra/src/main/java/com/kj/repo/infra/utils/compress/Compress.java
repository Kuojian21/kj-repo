package com.kj.repo.infra.utils.compress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author kj
 * Created on 2020-03-14
 */
public interface Compress {
    byte[] compress(byte[] src);

    byte[] decompress(byte[] src);

    OutputStream compress(OutputStream out) throws IOException;

    InputStream decompress(InputStream is) throws IOException;
}
