package com.kj.repo.infra.function;

@FunctionalInterface
public interface Supplier<T> {
    T get() throws Exception;
}