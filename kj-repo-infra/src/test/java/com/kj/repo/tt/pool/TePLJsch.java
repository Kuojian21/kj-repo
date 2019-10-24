package com.kj.repo.tt.pool;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;

import com.kj.repo.infra.pool.jsch.PLJsch;

public class TePLJsch {

    public void upload() throws Exception {
        PLJsch pLJsch = PLJsch.jsch("trans.kaifae.com", 2022, "LMLC", "9X9XWa$f");
        pLJsch.upload("/upload/test", "text.txt", new ByteArrayInputStream(new String("Hello World!").getBytes()));
        pLJsch.download("/upload/test", "test.txt", new FileOutputStream(new File("test.txt")));
    }

    public void upload2() throws Exception {
        PLJsch pLJsch = PLJsch.jsch("123.57.157.2", 22, "lmlctest", "classes/exchange_njjjs.pub",
                "classes/exchange_njjjs.ppk", "njjjs".getBytes("UTF-8"));
        pLJsch.upload("upload", "test.txt", new ByteArrayInputStream(new String("Hello World!123").getBytes()));
    }

    public void upload3() throws Exception {
        PLJsch pLJsch = PLJsch.jsch("trans.kaifae.com", 2022, "LMLC", "9X9XWa$f");
        pLJsch.upload("upload", "test.txt", new ByteArrayInputStream(new String("Hello World!123").getBytes()));
    }

}
