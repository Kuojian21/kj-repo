package com.kj.repo.infra.tool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.kj.repo.infra.bean.BeanSupplier;

/**
 * @author kuojian21
 */
public class TLSearch {

    public static <T> void traverse(Node<T> node, Predicate<T> predicate, Consumer<T> consumer) {
        if (node == null) {
            return;
        } else if (!predicate.test(node.get())) {
            return;
        }
        switch (node.type()) {
            case LEAF:
                consumer.accept(node.get());
                break;
            case COMP:
                consumer.accept(node.get());
                List<Node<T>> nodes = node.getChilds();

                int len = nodes.size();
                if (len > 0) {
                    IntStream.range(0, len - 1).boxed().forEach(i -> {
                        Holder.submit(() -> traverse(nodes.get(i), predicate, consumer));
                    });
                    traverse(nodes.get(len - 1), predicate, consumer);
                }
                break;
        }
    }

    public static void searchFile(File file, String name) {
        TLSearch.traverse(new FileNode(file), t -> true, t -> {
            if (t.getName().indexOf(name) >= 0) {
                System.out.println(file.getAbsolutePath());
            }
            if (file.getName().endsWith(".zip") || file.getName().endsWith(".jar")) {
                try {
                    TLZipFile.entry(file, e -> {
                        if (e.getName().indexOf(name) >= 0) {
                            System.out.println(file.getAbsolutePath() + "###" + e.getName());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void searchText(File file, String text) {
        TLSearch.traverse(new FileNode(file), t -> true, t -> {
            try {
                if (file.getName().endsWith(".zip") || file.getName().endsWith(".jar")) {
                    TLZipFile.entry(file, (e, i) -> {
                        if (Joiner.on("\n").join(TLStream.readLines(i)).indexOf(text) > 0) {
                            System.out.print(file.getAbsolutePath() + "###" + e.getName());
                        }
                    });
                } else {
                    if (Joiner.on("\n").join(TLStream.readLines(file.getAbsolutePath())).indexOf(text) >= 0) {
                        System.out.println(file.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public interface Node<T> {

        List<Node<T>> getChilds();

        NodeType type();

        T get();

        enum NodeType {
            LEAF, COMP
        }

    }

    private static class Holder {

        static AtomicLong count = new AtomicLong(0);
        static AtomicBoolean shutdown = new AtomicBoolean(false);
        static BeanSupplier<ExecutorService> service = new BeanSupplier<ExecutorService>(
                () -> new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                        Runtime.getRuntime().availableProcessors(), 0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>(4096), new ThreadPoolExecutor.CallerRunsPolicy()),
                s -> {
                    try {
                        s.shutdown();
                        s.awaitTermination(3, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

        static {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    shutdown.set(true);
                    service.close();
                } catch (Exception e) {

                }
            }));
        }

        static void submit(Runnable runnable) {
            if (shutdown.get()) {
                throw new RuntimeException();
            }
            Holder.count.incrementAndGet();
            service.get().submit(() -> {
                runnable.run();
                Holder.count.decrementAndGet();
            });
        }

    }

    public static class FileNode implements Node<File> {

        private final File file;

        public FileNode(File file) {
            super();
            this.file = file;
        }

        @Override
        public List<Node<File>> getChilds() {
            if (this.file.isFile()) {
                return Lists.newArrayList();
            }
            List<Node<File>> nodes = null;
            try {
                File[] files = file.listFiles();
                nodes = new ArrayList<Node<File>>(files.length);
                for (int i = 0; i < files.length; i++) {
                    if (files[i].getParentFile().getCanonicalPath().equals(this.file.getCanonicalPath())) {
                        nodes.add(new FileNode(files[i]));
                    }
                }

            } catch (IOException e) {

            }
            return nodes;
        }

        @Override
        public NodeType type() {
            return this.file.isFile() ? NodeType.LEAF : NodeType.COMP;
        }

        @Override
        public File get() {
            return this.file;
        }

    }
}
