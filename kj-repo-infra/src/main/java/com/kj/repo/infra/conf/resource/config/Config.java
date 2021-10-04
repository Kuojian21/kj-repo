package com.kj.repo.infra.conf.resource.config;

import java.util.function.Function;

import com.kj.repo.infra.conf.Conf;
import com.kj.repo.infra.conf.resource.ResourceRepository;

/**
 * @author kj
 * Created on 2020-09-30
 */
public interface Config<T> extends Conf<T> {

    @Override
    default T get() {
        return ResourceRepository.get(Config.class, this).get();
    }

    Function<byte[], T> decode();
}
