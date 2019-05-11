package com.kj.repo.spring.boot.controller;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author kuojian21
 */
@RestController
public class RangeController {

    private Logger logger = LoggerFactory.getLogger(RangeController.class);

    List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
    int chunksize = 1024 * 1024 * 1024;
    long size = 0;
    int chunk = 0;

    @PostConstruct
    public void init() throws IOException {
//		bytes = Files.readAllBytes(Paths.get("/Users/kuojian21", "2.mkv"));
        FileChannel fileChannel = FileChannel.open(Paths.get("/Users/kuojian21", "1.mp4"));
        size = fileChannel.size();
        chunk = (int) (size / 200);
        ByteBuffer buffer = ByteBuffer.allocate(chunksize);
        while (fileChannel.read(buffer) > 0) {
            buffers.add(buffer);
            buffer = ByteBuffer.allocate(chunksize);
        }
        fileChannel.close();
    }

    @RequestMapping(path = "/fragment")
    public ResponseEntity<byte[]> fragment(HttpServletRequest request) throws IOException {
        String range = request.getHeader("Range");
        logger.info("{}", range);

        long pos = 0;
        long end = chunk - 1;
        if (!Strings.isBlank(range)) {
            range = range.replaceAll("bytes=", "");
            String[] r = range.split("-");
            pos = Long.parseLong(r[0]);
            end = pos + chunk - 1;
            if (end > size) {
                end = size - 1;
            }
        } else {
            ResponseEntity<byte[]> entity = ResponseEntity.status(HttpStatus.PARTIAL_CONTENT) //
                    .header("Accept-Ranges", "bytes")//
                    .contentLength(0) //
                    .contentType(MediaType.valueOf("video/mp4")) //
                    .cacheControl(CacheControl.maxAge(10, TimeUnit.SECONDS).cachePublic()) //
                    .eTag("key") //
                    .body(null);
            return entity;
        }
        byte[] data = subarray(pos, end);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT) //
                .header("Content-Range", "bytes " + pos + "" + "-" + end + "" + "/" + size + "")//
                .header("Accept-Ranges", "bytes")//
                .contentLength(data.length) //
                .contentType(MediaType.valueOf("video/mp4")) //
                .cacheControl(CacheControl.maxAge(10, TimeUnit.DAYS).cachePublic()) //
                .eTag("key") //
                .body(data);
    }

    public byte[] subarray(long pos, long end) {
        if ((pos + 1) / chunksize != (end + 1) / chunksize) {
            end = (pos + 1) / chunksize * chunksize - 1;
        }
        int len = (int) (end - pos + 1);
        byte[] data = new byte[len];
        logger.info("{} {}", (int) (pos / chunksize), buffers.size());
        System.arraycopy(buffers.get((int) (pos / chunksize)).array(), (int) (pos / chunksize), data, 0, len);
        return data;
    }

}
