package com.kj.repo.infra;

/**
 * @author kj
 */
@FunctionalInterface
public interface Supplier<T> {
    T get() throws Exception;
}