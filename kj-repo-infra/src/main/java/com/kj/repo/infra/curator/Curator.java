package com.kj.repo.infra.curator;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kj.repo.infra.base.LocalSupplier;
import com.kj.repo.infra.helper.RunHelper;

/**
 * @author kj
 */
public class Curator<T> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final CuratorConf<T> conf;
    private final LocalSupplier<NodeCache> nodecache;
    private final LocalSupplier<T> delegate;
    private final BiConsumer<T, T> trigger;
    private final Executor executor;

    protected Curator(CuratorConf<T> conf) {
        this.conf = conf;
        this.trigger = conf.trigger();
        this.executor = conf.executor();
        this.nodecache = new LocalSupplier<>(() -> RunHelper.run(() -> {
            NodeCache nodecache = new NodeCache(conf.curator(), conf.path());
            nodecache.start();
            nodecache.getListenable().addListener(this::trigger);
            nodecache.rebuild();
            return nodecache;
        }));
        this.delegate = new LocalSupplier<>(
                () -> Optional.ofNullable(nodecache.get().getCurrentData())
                        .map(ChildData::getData)
                        .map(conf::decode)
                        .orElseGet(conf::defaultValue)
        );
    }

    public final void trigger() {
        this.executor.execute(() -> {
            T oldValue = this.get();
            this.delegate.refresh();
            this.trigger.accept(oldValue, this.get());
        });
    }

    public void set(T data) {
        CuratorConf<T> conf = this.conf;
        try {
            conf.curator().setData().forPath(conf.path(), conf.encode(data));
        } catch (KeeperException.NoNodeException e) {
            try {
                conf.curator().create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(conf.path(), conf.encode(data));
            } catch (KeeperException.NodeExistsException nee) {
                logger.info("concurrent happen");
            } catch (Exception ee) {
                throw new RuntimeException(ee);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public T get() {
        return this.delegate.get();
    }

    public CuratorConf<T> getConf() {
        return this.conf;
    }
}
