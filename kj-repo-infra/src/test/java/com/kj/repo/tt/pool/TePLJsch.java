package com.kj.repo.tt.pool;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.jcraft.jsch.SftpException;
import com.kj.repo.infra.pool.PLJsch;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TePLJsch {

	public void upload() throws Exception {
		PLJsch.Jsch.jsch("trans.kaifae.com", 2022, "LMLC", "9X9XWa$f").execute(sftp -> {
			try {
				PLJsch.Helper.upload(sftp, "/upload/test", "text.txt",
								new ByteArrayInputStream(new String("Hello World!").getBytes()));
				PLJsch.Helper.download(sftp, "/upload/test", "test.txt", new FileOutputStream(new File("test.txt")));
			} catch (SftpException | IOException e) {
				log.error("", e);
			}
		});

	}

	public void upload2() throws Exception {
		PLJsch.Jsch.jsch("123.57.157.2", 22, "lmlctest", "classes/exchange_njjjs.pub",
						"classes/exchange_njjjs.ppk", "njjjs".getBytes("UTF-8")).execute(sftp -> {
							try {
								PLJsch.Helper.upload(sftp, "upload", "test.txt",
												new ByteArrayInputStream(new String("Hello World!123").getBytes()));
							} catch (SftpException | IOException e) {
								log.error("", e);
							}
						});
	}

	public void upload3() throws Exception {
		PLJsch.Jsch.jsch("trans.kaifae.com", 2022, "LMLC", "9X9XWa$f").execute(sftp -> {
			try {
				PLJsch.Helper.upload(sftp, "upload", "test.txt",
								new ByteArrayInputStream(new String("Hello World!123").getBytes()));
			} catch (SftpException | IOException e) {
				log.error("", e);
			}
		});
	}

}
