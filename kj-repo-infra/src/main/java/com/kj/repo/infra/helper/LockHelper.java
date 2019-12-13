package com.kj.repo.infra.helper;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

public class LockHelper {

    public static void runInLock(Lock lock, Runnable runnable) {
        try {
            call(lock, false, () -> {
                runnable.run();
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runInTryLock(Lock lock, Runnable runnable) {
        try {
            call(lock, true, () -> {
                runnable.run();
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <T> T call(Lock lock, boolean tryLock, Callable<T> callable) throws Exception {
        if (tryLock) {
            lock.tryLock();
        } else {
            lock.lock();
        }
        try {
            return callable.call();
        } finally {
            lock.unlock();
        }
    }

}
