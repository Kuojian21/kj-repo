package com.kj.repo.tt.aop;

import com.kj.repo.infra.spring.BeanFactory;

public class AopTest {

	public static void main(String[] args) {
		AopTarget aopTarget = BeanFactory.getBean(AopTarget.class);
		aopTarget.target();
	}

}
