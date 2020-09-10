package com.kj.repo.infra.share;

import java.util.concurrent.ThreadFactory;

/**
 * @author kj
 * Created on 2020-09-10
 */
public class ShareThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName("share-pool");
        thread.setDaemon(true);
        return thread;
    }
}
