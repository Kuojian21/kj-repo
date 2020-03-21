package com.kj.repo.infra.base.function;

/**
 * @author kj
 */
@FunctionalInterface
public interface Supplier<T> {
    T get() throws Exception;
}