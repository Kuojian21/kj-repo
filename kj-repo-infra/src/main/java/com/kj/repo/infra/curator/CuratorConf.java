package com.kj.repo.infra.curator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.kj.repo.infra.bean.LocalSupplier;
import com.kj.repo.infra.conf.Conf;
import com.kj.repo.infra.helper.RunnableHelper;

public abstract class CuratorConf<T> implements Conf<T> {
    private static ExecutorService executor = Executors.newCachedThreadPool();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String path;
    private final LocalSupplier<NodeCache> nodeCache;
    private final LocalSupplier<T> delegate;
    private final T defaultValue;

    public CuratorConf(String path, T defaultValue) {
        this.defaultValue = defaultValue;
        this.path = path;
        this.nodeCache = new LocalSupplier<NodeCache>(() -> {
            return RunnableHelper.call(() -> {
                NodeCache nodeCache = new NodeCache(curator(), path);
                nodeCache.start();
                nodeCache.getListenable().addListener(() -> {
                    CuratorConf.this.refresh();
                });
                nodeCache.rebuild();
                return nodeCache;
            });
        });
        this.delegate = new LocalSupplier<T>(() -> {
            return RunnableHelper.call(() -> {
                NodeCache nodeCache = this.nodeCache.get();
                ChildData data = nodeCache.getCurrentData();
                if (data != null) {
                    byte[] bytes = data.getData();
                    if (bytes != null) {
                        return this.decode(bytes);
                    }
                }
                return defaultValue;
            });
        });
    }

    public CuratorConf(LocalSupplier<NodeCache> nodeCache, LocalSupplier<T> delegate) {
        this.path = null;
        this.defaultValue = null;
        this.nodeCache = nodeCache;
        this.delegate = delegate;
    }

    public T get() {
        return this.delegate.get();
    }

    public <R> CuratorConf<R> mapper(Function<T, R> mapper) {
        LocalSupplier<R> mDelegate = new LocalSupplier<>(() -> {
            return mapper.apply(delegate.get());
        });
        LocalSupplier<NodeCache> mNodeCache = new LocalSupplier<NodeCache>(() -> {
            nodeCache.get().getListenable().addListener(() -> {
                mDelegate.refresh();
            });
            return nodeCache.get();
        });
        return new CuratorConf<R>(mNodeCache, mDelegate) {
            @Override
            public R decode(byte[] data) {
                throw new RuntimeException("not supported");
            }

            @Override
            public byte[] encode(R data) {
                throw new RuntimeException("not supported");
            }

            @Override
            public CuratorFramework curator() {
                throw new RuntimeException("not supported");
            }
        };
    }

    public void refresh() {
        executor.submit(this.delegate::refresh);
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

    public T defaultValue() {
        return this.defaultValue;
    }

    public abstract T decode(byte[] data);

    public abstract byte[] encode(T data);

    public abstract CuratorFramework curator();
}
