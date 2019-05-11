package com.kj.repo.tt.helper;

import java.io.IOException;
import java.nio.file.Paths;

import com.google.common.io.Files;
import com.kj.repo.infra.helper.ZxingHelper;

public class TeZxingHelper {

	public static void main(String[] args) throws IOException {
		ZxingHelper.matrix("https://www.lmlc.com");
		Files.write(ZxingHelper.matrix("https://www.lmlc.com"),
				Paths.get(System.getProperty("user.home") + "/text.jpg").toFile());
	}

}
