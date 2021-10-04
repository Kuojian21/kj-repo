package com.kj.repo.infra.conf.zk;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.kj.repo.infra.LocalSupplier;
import com.kj.repo.infra.conf.Conf;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

/**
 * @author kj
 */
public class Curator<T> implements Supplier<T> {
    private final String path;
    private final Conf<T> conf;
    private final CuratorFramework curator;
    private final LocalSupplier<T> delegate;
    private final Function<byte[], T> decode;

    public Curator(String prefix, Conf<T> conf, CuratorFramework curator, Function<byte[], T> decode,
            Consumer<T> release) {
        this.path = prefix + "." + conf.name();
        this.conf = conf;
        this.curator = curator;
        this.decode = decode;
        this.delegate = new LocalSupplier<>(() -> {
            try {
                NodeCache nodecache = new NodeCache(this.curator, this.path);
                nodecache.start();
                nodecache.getListenable().addListener(this::refresh);
                nodecache.rebuild();
                return Optional.ofNullable(nodecache.getCurrentData())
                        .map(ChildData::getData)
                        .map(this.decode)
                        .orElseGet(conf.defValue());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }, release);
    }

    @Override
    public T get() {
        return this.delegate.get();
    }

    public void set(byte[] bytes) {
        try {
            this.curator.setData().forPath(this.path, bytes);
        } catch (KeeperException.NoNodeException e) {
            try {
                this.curator.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(this.path, bytes);
            } catch (KeeperException.NodeExistsException nee) {
            } catch (Exception ee) {
                throw new RuntimeException(ee);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void refresh() {
        this.delegate.refresh();
    }
}
