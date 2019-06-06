package com.kj.repo.infra.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kj.repo.infra.bean.BeanSupplier;

/**
 * @param <T>
 * @author kuojian21
 */
public abstract class CuratorNode<T> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String path;
    private final T defaultValue;
    private final BeanSupplier<NodeCache> nodeCache;
    private final BeanSupplier<T> delegate;

    public CuratorNode(String path, T defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
        this.nodeCache = new BeanSupplier<>(() -> {
            NodeCache nodeCache = new NodeCache(this.curator(), path);
            nodeCache.start();
            nodeCache.getListenable().addListener(() -> {
                logger.info("data change");
                CuratorNode.this.delegate().reset();
            });
            nodeCache.rebuild();
            return nodeCache;
        }, NodeCache::close);
        this.delegate = new BeanSupplier<T>(() -> {
            logger.info("get data of {}", path);
            NodeCache nodeCache = this.nodeCache.get();
            ChildData data = nodeCache.getCurrentData();
            if (data != null) {
                byte[] bytes = data.getData();
                if (bytes != null) {
                    return this.decode(bytes);
                }
            }
            logger.info("the data of {} is null", path);
            return this.defaultValue;
        });
    }

    public T get() {
        return this.delegate.get();
    }

    public void set(T data) {
        try {
            curator().setData().forPath(this.path, encode(data));
        } catch (NoNodeException e) {
            try {
                curator().create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(this.path,
                        encode(data));
            } catch (NodeExistsException nee) {
                logger.info("concurrent happen");
            } catch (Exception ee) {
                throw new RuntimeException(ee);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract T decode(byte[] data);

    public abstract byte[] encode(T data);

    public abstract CuratorFramework curator();

    private BeanSupplier<T> delegate() {
        return this.delegate;
    }

}
