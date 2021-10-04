package com.kj.repo.infra.conf.resource;

import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.apache.curator.shaded.com.google.common.collect.Maps;

import com.kj.repo.infra.conf.Conf;

public class ResourceRepository {
	private static volatile ResourceRepository instance;
	private ConcurrentMap<Class<?>, Resource<?>> resources = Maps.newConcurrentMap();
	
	public static void register(Resource<?> resource) {
		if (getInstance().resources.putIfAbsent(resource.clazz(), resource) != null) {
			throw new RuntimeException();
		}
	}

	public static ResourceRepository getInstance() {
		if (instance == null) {
			synchronized (ResourceRepository.class) {
				if (instance == null) {
					instance = new ResourceRepository();
					instance.init();
				}
			}
		}
		return instance;
	}

	public static <T> Supplier<T> get(Class<?> clazz, Conf<T> conf) {
		Resource<T> resource = ResourceRepository.getInstance().get(clazz);
		return resource.get(conf);
	}

	@SuppressWarnings("unchecked")
	public <T> Resource<T> get(Class<?> clazz) {
		return (Resource<T>) resources.get(clazz);
	}

	private void init() {
		ServiceLoader.load(Resource.class).forEach(ResourceRepository::register);
	}
}
