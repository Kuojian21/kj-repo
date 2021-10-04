package com.kj.repo.infra.conf.resource;

import org.apache.curator.framework.CuratorFramework;

public abstract class ResourceCurator<T> extends Resource<T> {
	protected abstract CuratorFramework curator();
}
