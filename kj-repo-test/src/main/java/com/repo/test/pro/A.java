package com.repo.test.pro;

import com.google.common.base.Supplier;
import com.repo.test.pro.p.B;

public class A extends B {

	public static class C {
		public static Supplier<Object> c() {
			return B::b;
		}
	}

	public static void main(String[] args) {
		C.c();
	}
}
