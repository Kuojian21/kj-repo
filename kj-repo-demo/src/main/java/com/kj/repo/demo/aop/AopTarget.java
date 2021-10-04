package com.kj.repo.demo.aop;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.kj.repo.infra.logger.LoggerHelper;

@Service
public class AopTarget {

    private static final Logger logger = LoggerHelper.getLogger();

    public void target() {
        logger.info("{}", "target");
    }

}
