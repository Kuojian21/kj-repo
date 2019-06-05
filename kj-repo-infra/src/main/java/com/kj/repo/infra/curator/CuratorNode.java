package com.kj.repo.infra.curator;

import java.util.function.Supplier;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;

/**
 * @param <T>
 * @author kuojian21
 */
public abstract class CuratorNode<T> {

    private final String path;
    private final NodeSupplier<NodeCache> nodeCache;
    private final NodeSupplier<T> delegate;

    public CuratorNode(String path) {
        this.path = path;
        this.nodeCache = new NodeSupplier<>(() -> {
            NodeCache nodeCache = new NodeCache(this.curator(), path);
            nodeCache.start();
            nodeCache.getListenable().addListener(() -> {
                delegate().reset();
            });
            return nodeCache;
        }, NodeCache::close);
        this.delegate = new NodeSupplier<T>(() -> {
            NodeCache nodeCache = this.nodeCache.get();
            ChildData data = nodeCache.getCurrentData();
            return this.decode(data.getData());
        });
    }

    public void setData(T data) throws Exception {
        try {
            curator().setData().forPath(this.path, encode(data));
        } catch (NoNodeException e) {
            try {
                curator().create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(this.path,
                        encode(data));
            } catch (NodeExistsException ignored) {
            }
        }
    }

    public abstract T decode(byte[] data);

    public abstract byte[] encode(T data);

    public abstract CuratorFramework curator();

    private NodeSupplier<T> delegate() {
        return this.delegate;
    }

    public static class NodeSupplier<T> implements Supplier<T>, AutoCloseable {
        private final Supplier<T> delegate;
        private final Consumer<T> close;
        private volatile boolean initialized;
        private T value;

        public NodeSupplier(Supplier<T> delegate) {
            this(delegate, null);
        }

        public NodeSupplier(Supplier<T> delegate, Consumer<T> close) {
            super();
            this.delegate = delegate;
            this.close = close;
        }

        @Override
        public T get() {
            if (!this.initialized) {
                synchronized (this) {
                    if (!this.initialized) {
                        try {
                            this.value = this.delegate.get();
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                        this.initialized = true;
                    }
                }
            }
            return this.value;
        }

        public void reset() throws Exception {
            if (this.initialized) {
                if (this.close != null) {
                    close.accept(this.value);
                }
                this.value = this.delegate.get();
            }
        }

        @Override
        public void close() throws Exception {
            if (this.initialized && this.close != null) {
                close.accept(this.value);
            }
            this.initialized = false;
            this.value = null;
        }

        @FunctionalInterface
        public interface Supplier<T> {
            T get() throws Exception;
        }

        @FunctionalInterface
        public interface Consumer<T> {
            void accept(T t) throws Exception;
        }

    }

}
