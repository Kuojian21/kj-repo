package com.kj.repo.infra.conf.curator;

import java.util.concurrent.ConcurrentMap;

import org.apache.curator.shaded.com.google.common.collect.Maps;

public class CuratorHelper {

	private static final ConcurrentMap<String, Class<?>> CACHE = Maps.newConcurrentMap();

	public static String check(String prefix, Class<?> clazz) {
		Class<?> oclazz = CACHE.putIfAbsent(prefix, clazz);
		if (oclazz == null || oclazz == clazz) {
			return prefix;
		}
		throw new RuntimeException();
	}
}
