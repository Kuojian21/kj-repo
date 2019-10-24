package com.kj.repo.infra.function;

/**
 * @author kuojian21
 */
@FunctionalInterface
public interface Consumer<T> {
    void accept(T t) throws Exception;
}