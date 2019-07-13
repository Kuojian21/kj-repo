package com.kj.repo.tt.aop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AopTarget {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void target() {
        logger.info("{}", "target");
    }

}
