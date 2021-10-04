package com.kj.repo.demo.aop;

import org.slf4j.Logger;

import com.kj.repo.infra.logger.LoggerHelper;

/**
 * @author kj
 */
public class AopAspect {

    private static final Logger logger = LoggerHelper.getLogger();

    public void before() {
        logger.info("{}", "before");
    }

    public void after() {
        logger.info("{}", "after");
    }

}
