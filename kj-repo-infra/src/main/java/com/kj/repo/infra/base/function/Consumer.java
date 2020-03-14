package com.kj.repo.infra.base.function;

/**
 * @author kj
 */
@FunctionalInterface
public interface Consumer<T> {
    void accept(T t) throws Exception;
}