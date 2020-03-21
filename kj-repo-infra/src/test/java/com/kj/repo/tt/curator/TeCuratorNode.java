package com.kj.repo.tt.curator;

import java.util.Random;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;

import com.kj.repo.infra.curator.CuratorConf;
import com.kj.repo.infra.logger.LoggerHelper;

/**
 *
 */
public class TeCuratorNode {

    private static final Logger logger = LoggerHelper.getLogger();

    public static void main(String[] args) throws InterruptedException {
        CuratorFramework curator = CuratorFrameworkFactory.builder()
                .connectString(args[1])
                .retryPolicy(new RetryNTimes(1, 1000))
                .sessionTimeoutMs(60000)
                .connectionTimeoutMs(50000)
                .build();
        curator.start();
        CuratorConf<String> node = new CuratorConf<String>() {
            @Override
            public String path() {
                return args[0];
            }

            @Override
            public String defaultValue() {
                return null;
            }

            @Override
            public String decode(byte[] data) {
                return new String(data);
            }

            @Override
            public byte[] encode(String data) {
                return data.getBytes();
            }

            @Override
            public CuratorFramework curator() {
                return curator;
            }
        };

        Random random = new Random();
        while (true) {
            logger.info("{}", node.get());
            if (random.nextInt(10) == 1) {
                node.set(random.nextInt() + "");
            }
            Thread.sleep(1000);
        }
    }

}
