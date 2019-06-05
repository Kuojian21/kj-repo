package com.kj.repo.infra.curator;

import org.apache.curator.framework.CuratorFramework;

public abstract class CuratorTree<T> {

    public boolean lock() {

        return true;
    }

    public void unlock() {

    }

    public abstract CuratorFramework curator();

}
