package com.kj.repo.infra.base.function;

@FunctionalInterface
public interface Supplier<T> {
    T get() throws Exception;
}