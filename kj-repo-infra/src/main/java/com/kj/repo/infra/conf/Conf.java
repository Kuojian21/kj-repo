package com.kj.repo.infra.conf;

import java.util.function.Supplier;

public interface Conf<T> extends Supplier<T> {

    String name();

    default Supplier<T> defValue() {
        return () -> null;
    }
}
