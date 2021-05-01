package com.kj.repo.infra.conf.resource.oracle;

import com.kj.repo.infra.conf.model.Holder;

/**
 * @author kj
 * Created on 2020-09-30
 */
public class OracleHolder extends Holder {
    public static OracleHolder of(OracleConfig config) {
        return new OracleHolder();
    }

    @Override
    public void close() {

    }
}
