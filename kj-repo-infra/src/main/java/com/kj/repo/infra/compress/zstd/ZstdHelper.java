package com.kj.repo.infra.compress.zstd;

import java.io.File;
import java.io.IOException;

import org.apache.curator.shaded.com.google.common.io.ByteProcessor;
import org.apache.curator.shaded.com.google.common.io.Files;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdDictTrainer;

/**
 * @author kj
 * Created on 2020-03-14
 */
public class ZstdHelper {
    public static byte[] dict(byte[][] trainBytes, int samplesize, int dictsize) {
        ZstdDictTrainer zstdDictTrainer = new ZstdDictTrainer(samplesize, dictsize);
        for (byte[] sample : trainBytes) {
            zstdDictTrainer.addSample(sample);
        }
        return zstdDictTrainer.trainSamples(true);
    }

    public static byte[] dict(File file, int samplesize, int dictsize) throws IOException {
        ZstdDictTrainer zstdDictTrainer = new ZstdDictTrainer(samplesize, dictsize);
        Files.readBytes(file, new ByteProcessor<Void>() {
            @Override
            public boolean processBytes(byte[] bytes, int i, int i1) throws IOException {
                byte[] sBytes = bytes;
                if (i != 0 || i1 != bytes.length) {
                    sBytes = new byte[i1];
                    System.arraycopy(bytes, i, sBytes, 0, i1);
                }
                if (!zstdDictTrainer.addSample(bytes)) {
                    throw new RuntimeException("");
                }
                return true;
            }

            @Override
            public Void getResult() {
                return null;
            }
        });
        return zstdDictTrainer.trainSamples(true);
    }

    public static byte[] dict(byte[][] trainBytes) {
        byte[] tDict = new byte[1024 * 1024];
        int dictlengh = (int) Zstd.trainFromBuffer(trainBytes, tDict);
        byte[] dict = new byte[dictlengh];
        System.arraycopy(tDict, 0, dict, 0, dictlengh);
        return dict;
    }


}
