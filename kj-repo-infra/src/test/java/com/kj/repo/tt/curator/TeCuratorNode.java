package com.kj.repo.tt.curator;

import java.util.Random;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kj.repo.infra.curator.CuratorConf;

/**
 *
 */
public class TeCuratorNode {

    private static Logger logger = LoggerFactory.getLogger(TeCuratorNode.class);

    public static void main(String[] args) throws InterruptedException {
        CuratorConf<String> node = new CuratorConf<String>(args[0], null) {

            private CuratorFramework curator = CuratorFrameworkFactory.builder().connectString(args[1])
                    .retryPolicy(new RetryNTimes(1, 1000)).sessionTimeoutMs(60000).connectionTimeoutMs(50000).build();

            {
                curator.start();
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
