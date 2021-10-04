package com.kj.repo.demo.zxing;

import java.io.IOException;
import java.nio.file.Paths;

import com.google.common.io.Files;

public class TeZxingHelper {

    public static void main(String[] args) throws IOException {
        Files.write(ZxingHelper.encode("https://www.lmlc.com"),
                Paths.get(System.getProperty("user.home") + "/text.jpg").toFile());

        System.out.println(ZxingHelper.decode(
                Files.toByteArray(
                        Paths.get(System.getProperty("user.home") + "/Desktop/Screen Shot 2019-06-12 at 17.25.51.png")
                                .toFile())));
    }

}
