package com.kj.repo.tt.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.kj.repo.infra.spring.BeanFactory;

/**
 *
 */
@Service
public class SpringTest {
    public final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static void main(String[] args) {
        BeanFactory.getBean(SpringTest.class).test();
    }

    public void test() {
        logger.info("{}", "");
    }
}
