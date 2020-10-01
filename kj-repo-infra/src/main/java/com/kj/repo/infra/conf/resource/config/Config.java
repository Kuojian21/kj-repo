package com.kj.repo.infra.conf.resource.config;

import java.util.function.Function;

import com.kj.repo.infra.conf.base.Conf;
import com.kj.repo.infra.conf.register.RegisterHelper;

/**
 * @author kj
 * Created on 2020-09-30
 */
public interface Config<T> extends Conf<T> {

    @Override
    default T get() {
        return RegisterHelper.config(this).get();
    }

    Function<byte[], T> decode();
}
