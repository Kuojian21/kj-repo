package com.kj.repo.tt.aop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AopAspect {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void before() {
        logger.info("{}", "before");
    }

    public void after() {
        logger.info("{}", "after");
    }

}
