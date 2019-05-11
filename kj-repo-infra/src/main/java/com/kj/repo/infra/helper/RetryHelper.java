package com.kj.repo.infra.helper;

import com.google.common.base.Predicate;

/**
 * @author kuojian21
 */
public class RetryHelper {

    public static <T, R> R retry(Function<T, R> function, T input, int times,
                                 int sleep) throws Throwable {
        return retry(function, p -> {
            try {
                Thread.sleep(sleep);
                return true;
            } catch (InterruptedException e) {
                return false;
            }
        }, input, times);
    }

    public static <T, R> R retry(Function<T, R> function, Predicate<Throwable> predicate, T input,
                                 int times) throws Throwable {
        Throwable t = null;
        for (int i = 0; i < times; i++) {
            try {
                return function.apply(input);
            } catch (Throwable ex) {
                if (!predicate.apply(ex)) {
                    throw t;
                }
                t = ex;
            }
        }
        throw t;
    }

    /**
     * @author kuojian21
     */
    @FunctionalInterface
    public static interface Function<T, R> {
        R apply(T t) throws Exception;
    }

}
