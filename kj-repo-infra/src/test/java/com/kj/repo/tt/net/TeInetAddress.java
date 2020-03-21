package com.kj.repo.tt.net;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;

import com.kj.repo.infra.logger.LoggerHelper;

/**
 * @author kj
 * Created on 2020-03-21
 */
public class TeInetAddress {
    public static final Logger logger = LoggerHelper.getLogger();

    public static void main(String[] args) throws UnknownHostException {
        for (int i = 0; i < 10; i++) {
            logger.info("{}", InetAddress.getLocalHost());
        }
    }
}
