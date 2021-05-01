package com.kj.repo.infra.conf.model;

/**
 * @author kj
 * Created on 2020-10-01
 */
public class Endpoint {
    private String host;
    private int port;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
