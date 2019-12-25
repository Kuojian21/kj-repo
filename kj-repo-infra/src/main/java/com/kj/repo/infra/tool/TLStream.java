package com.kj.repo.infra.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * @author kj
 */
public class TLStream {

    public static byte[] readBytes(String path, String... file) throws IOException {
        return Files.readAllBytes(Paths.get(path, file));
    }

    public static List<String> readLines(String path, String... file) throws IOException {
        return Files.readAllLines(Paths.get(path, file));
    }

    public static List<String> readLines(InputStream is) throws IOException {
        List<String> lines = Lists.newArrayList();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;
        while ((line = br.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }

}
