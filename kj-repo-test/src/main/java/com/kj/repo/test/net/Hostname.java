package com.kj.repo.test.net;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author kj
 * Created on 2020-03-29
 */
public class Hostname {
    public static void main(String[] args) throws UnknownHostException {
        for (int i = 0; i < 100; i++) {
            System.out.println(InetAddress.getLocalHost());
        }
    }
}
