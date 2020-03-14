package com.kj.repo.tt.net.ftp;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;

import com.kj.repo.infra.net.ftp.Jsch;

/**
 * @author kj
 */
public class TeJsch {

    public static void main(String[] args) throws Exception {
        doExecute(Jsch.jsch("trans.kaifae.com", 2022, "LMLC", "9X9XWa$f"));
        doExecute(Jsch.jsch("123.57.157.2", 22, "lmlctest", "classes/exchange_njjjs.pub",
                "classes/exchange_njjjs.ppk", "njjjs".getBytes("UTF-8")));
    }

    public static void doExecute(Jsch jsch) throws Exception {
        jsch.doExecute(sftp -> {
            sftp.mkdir("/upload/test");
            sftp.cd("/upload/test");
            sftp.put(new ByteArrayInputStream("Hello World!".getBytes()), "text.txt");
        });
        jsch.doExecute(sftp -> {
            sftp.cd("/upload/test");
            sftp.get("text.txt", new FileOutputStream("test.txt"));
        });
    }
}
