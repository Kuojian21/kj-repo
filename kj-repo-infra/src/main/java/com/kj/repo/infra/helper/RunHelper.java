package com.kj.repo.infra.helper;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.kj.repo.infra.logger.LoggerHelper;
import com.kj.repo.infra.perf.PerfHelper;

/**
 * @author kj
 */
public class RunHelper {

    private static final Logger logger = LoggerHelper.getLogger();

    public static void run(Runnable runnable) {
        run(() -> {
            runnable.run();
            return null;
        });
    }

    public static void run(Runnable runnable, String namespace, String tag, Object... extras) {
        run(() -> {
            runnable.run();
            return null;
        }, namespace, tag, extras);
    }

    public static <T> T run(Callable<T> callable) {
        return run(callable, null, null);
    }

    public static <T> T run(Callable<T> callable, String namespace, String tag, Object... extras) {
        try {
            return call(callable, namespace, tag, extras);
        } catch (Throwable e) {
            logger.info("", e);
            return null;
        }
    }

    public static void call(Runnable runnable) throws Throwable {
        call(() -> {
            runnable.run();
            return null;
        });
    }

    public static void call(Runnable runnable, String namespace, String tag, Object... extras) throws Throwable {
        call(() -> {
            runnable.run();
            return null;
        }, namespace, tag, extras);
    }

    public static <T> T call(Callable<T> callable) throws Throwable {
        return call(callable, null, null);
    }

    public static <T> T call(Callable<T> callable, String namespace, String tag, Object... extras) throws Throwable {
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<Object> tExtras = Lists.newArrayList();
        String flag = "";
        try {
            T rtn = callable.call();
            flag = "succ";
            return rtn;
        } catch (Throwable e) {
            flag = "fail";
            throw e;
        } finally {
            if (Strings.isNotEmpty(namespace)) {
                tExtras.add(flag);
                if (extras != null) {
                    tExtras.addAll(Arrays.asList(extras));
                }
                PerfHelper.perf(namespace, tag, tExtras.toArray()).count(1)
                        .micro(stopwatch.elapsed(TimeUnit.MICROSECONDS)).logstash();
            }
        }
    }
}
