package com.kj.repo.infra.base.conf;

public interface Conf<T> {

    String path();

    T defaultValue();

    T decode(byte[] data);

    byte[] encode(T data);
}
