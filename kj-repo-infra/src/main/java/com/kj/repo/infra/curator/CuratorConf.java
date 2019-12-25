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
import com.kj.repo.infra.base.LocalSupplier;
import com.kj.repo.infra.conf.Conf;
import com.kj.repo.infra.helper.RunHelper;

public abstract class CuratorConf<T> implements Conf<T> {
    private static LocalSupplier<ExecutorService> asyncExecutor = new LocalSupplier<>(
            () -> Executors.newFixedThreadPool(2));

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String path;
    private final LocalSupplier<NodeCache> nodecache;
    private final LocalSupplier<T> delegate;
    private final T defaultValue;

    public CuratorConf(String path, T defaultValue) {
        this.defaultValue = defaultValue;
        this.path = path;
        this.nodecache = new LocalSupplier<>(() -> RunHelper.run(() -> {
            NodeCache nodecache = new NodeCache(curator(), path);
            nodecache.start();
            nodecache.getListenable().addListener(CuratorConf.this::refresh);
            nodecache.rebuild();
            return nodecache;
        }));
        this.delegate = new LocalSupplier<>(() -> RunHelper.run(() -> {
            NodeCache nodeCache = this.nodecache.get();
            ChildData data = nodeCache.getCurrentData();
            if (data != null) {
                byte[] bytes = data.getData();
                if (bytes != null) {
                    return this.decode(bytes);
                }
            }
            return defaultValue;
        }), defaultValue);
    }

    public CuratorConf(LocalSupplier<NodeCache> nodeCache, LocalSupplier<T> delegate) {
        this.path = null;
        this.defaultValue = null;
        this.nodecache = nodeCache;
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
            nodecache.get().getListenable().addListener(() -> {
                mDelegate.refresh();
            });
            return nodecache.get();
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
        asyncExecutor.get().submit(this.delegate::refresh);
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
