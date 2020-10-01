package com.kj.repo.tt.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;

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
    }

}
