package com.kj.repo.infra.helper;

import java.util.function.Predicate;

import com.kj.repo.infra.base.function.Function;

/**
 * @author kj
 */
public class RetryHelper {

    public static <T, R> R retry(Function<T, R> function, T input, int times, int sleep) throws Throwable {
        return retry(function, p -> true, p -> {
            try {
                Thread.sleep(sleep);
                return true;
            } catch (InterruptedException e) {
                return false;
            }
        }, input, times);
    }

    public static <T, R> R retry(Function<T, R> function, Predicate<R> rPredicate, Predicate<Throwable> tPredicate,
            T input, int times) throws Throwable {
        Throwable t = null;
        for (int i = 0; i < times; i++) {
            try {
                R r = function.apply(input);
                if (!rPredicate.test(r)) {
                    return r;
                }
            } catch (Throwable ex) {
                if (!tPredicate.test(ex)) {
                    throw t;
                }
                t = ex;
            }
        }
        throw t;
    }
}
