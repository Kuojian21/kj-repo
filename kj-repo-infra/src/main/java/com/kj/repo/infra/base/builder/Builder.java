package com.kj.repo.infra.base.builder;

/**
 * @author kj
 */
public interface Builder<T> {

    T doBuild();

    default T build() {
        ensure();
        return doBuild();
    }

    default void ensure() {

    }

}
