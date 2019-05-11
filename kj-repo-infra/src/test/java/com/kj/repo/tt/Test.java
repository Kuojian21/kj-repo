package com.kj.repo.tt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Test {

    public static void main(String[] args) throws IOException {
        List<String> list = Files.readAllLines(Paths.get("/Users/kuojian21/b.txt"));

        for (String line : list) {
            String[] s = line.split("\\|");
            System.out.println(s[1].trim() + "," + s[2].trim() + "," + (999900000 + (Integer.parseInt(s[3].trim()) << 7)
                    + (Integer.parseInt(s[5].trim()) << 3) + Integer.parseInt(s[4].trim())));
        }

    }

}
