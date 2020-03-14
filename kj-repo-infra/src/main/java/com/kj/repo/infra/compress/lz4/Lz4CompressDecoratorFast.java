package com.kj.repo.infra.compress.lz4;

/**
 * @author kj
 * Created on 2020-03-14
 */
public class Lz4CompressDecoratorFast extends Lz4CompressDecorator {

    public Lz4CompressDecoratorFast(Lz4Compress delegate) {
        super(delegate);
    }


}
