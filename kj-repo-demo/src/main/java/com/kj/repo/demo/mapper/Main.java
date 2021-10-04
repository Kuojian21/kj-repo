package com.kj.repo.demo.mapper;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel.MapMode;

/**
 * @author kj
 * Created on 2020-09-08
 */
public class Main {
    public static void main(String[] args) throws IOException {
        RandomAccessFile file = new RandomAccessFile("", "r");
        file.getChannel().map(MapMode.READ_ONLY, 0L, file.length());
    }
}