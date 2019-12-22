package com.kj.repo.infra.builder;

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
