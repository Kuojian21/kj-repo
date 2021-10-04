package com.kj.repo.infra;

/**
 * @author kj
 * Created on 2020-03-14
 */
public interface BiFunction<T, U, R> {
    R apply(T t, U u) throws Exception;
}
