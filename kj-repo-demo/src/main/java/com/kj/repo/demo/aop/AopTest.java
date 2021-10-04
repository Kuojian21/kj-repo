package com.kj.repo.demo.aop;

import com.kj.repo.infra.utils.SpringBeanFactoryUtil;

public class AopTest {

    public static void main(String[] args) {
        AopTarget aopTarget = SpringBeanFactoryUtil.getBean(AopTarget.class);
        aopTarget.target();
    }

}
