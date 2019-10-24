package com.kj.repo.infra.helper;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author kuojian21
 */
public class RunnableHelper {

    public static Logger logger = LoggerFactory.getLogger(RunnableHelper.class);

    public static <T> T call(Callable<T> callable) {
        return call(callable, false);
    }

    public static void call(Runnable runnable) {
        run(runnable, false);
    }

    public static <T> T call(Callable<T> callable, boolean throwException) {
        try {
            return callable.call();
        } catch (Exception e) {

            if (throwException) {
                throw new RuntimeException(RunnableHelper.class.getName(), e);
            }
            logger.error("", e);
            return null;
        }
    }

    public static void run(Runnable runnable, boolean throwException) {
        try {
            runnable.run();
        } catch (Exception e) {
            if (throwException) {
                throw new RuntimeException(RunnableHelper.class.getName(), e);
            }
            logger.error("", e);
        }
    }

    public static interface Runnable {
        void run() throws Exception;
    }

}
