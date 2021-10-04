package com.kj.repo.infra;

/**
 * @author kj
 */
@FunctionalInterface
public interface Consumer<T> {
    void accept(T t) throws Exception;
}