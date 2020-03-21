package com.kj.repo.tt.spring;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.kj.repo.infra.logger.LoggerHelper;
import com.kj.repo.infra.spring.BeanFactory;

/**
 *
 */
@Service
public class SpringTest {
    private static final Logger logger = LoggerHelper.getLogger();

    public static void main(String[] args) {
        BeanFactory.getBean(SpringTest.class).test();
    }

    public void test() {
        logger.info("{}", "");
    }
}
