package com.kj.repo.infra.function;

/**
 * @author kuojian21
 */
@FunctionalInterface
public interface Function<T, R> {
    R apply(T t) throws Exception;
}