package com.kj.repo.infra;

/**
 * @author kj
 * Created on 2020-03-14
 */
@FunctionalInterface
public interface BiConsumer<T, U> {
    void accept(T t, U u) throws Exception;
}