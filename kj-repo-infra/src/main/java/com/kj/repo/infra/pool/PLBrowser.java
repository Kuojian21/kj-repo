package com.kj.repo.infra.pool;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.google.common.collect.Lists;

import io.reactivex.Observable;
import lombok.Data;

/**
 * @author kuojian21 http://htmlunit.sourceforge.net/
 */
public class PLBrowser<T> {

    private final GenericObjectPool<T> pool;

    public PLBrowser(final GenericObjectPool<T> pool) {
        this.pool = pool;
    }

    public static PLBrowser<WebClient> browser(BrowserVersion version) {
        return new PLBrowser<WebClient>(new GenericObjectPool<WebClient>(new BasePooledObjectFactory<WebClient>() {
            @Override
            public PooledObject<WebClient> wrap(WebClient webClient) {
                return new DefaultPooledObject<WebClient>(webClient);
            }

            @Override
            public WebClient create() throws Exception {
                WebClient webClient = new WebClient(version);
                webClient.getOptions().setThrowExceptionOnScriptError(false);
                webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
                webClient.getOptions().setActiveXNative(true);
                webClient.getOptions().setCssEnabled(true);
                webClient.getOptions().setJavaScriptEnabled(true);
                webClient.getOptions().setRedirectEnabled(true);
                webClient.getOptions().setActiveXNative(true);
                webClient.setAjaxController(new NicelyResynchronizingAjaxController());
                return webClient;
            }

            @Override
            public void destroyObject(final PooledObject<WebClient> obj)
                    throws Exception {
                obj.getObject().close();
            }
        }));
    }

    public final <R> R execute(Function<T, R> function) throws Exception {
        T t = null;
        try {
            t = this.pool.borrowObject();
            return function.apply(t);
        } finally {
            if (t != null) {
                this.pool.returnObject(t);
            }
        }
    }

    public final void execute(Consumer<T> consumer) throws Exception {
        T t = null;
        try {
            t = this.pool.borrowObject();
            consumer.accept(t);
        } finally {
            if (t != null) {
                this.pool.returnObject(t);
            }
        }
    }

    public final void shutdown() {
        this.pool.close();
    }

    @FunctionalInterface
    public static interface Function<T, R> {
        R apply(T t) throws Exception;
    }

    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T t) throws Exception;
    }

    /**
     * @author kuojian21
     */
    public static class Helper {

        @SuppressWarnings("unchecked")
        public static <N, T> List<T> parse(HtmlPage page, String xpath) {
            List<T> result = Lists.newArrayList();
            List<N> nodes = page.getByXPath(xpath);
            URL url = page.getBaseURL();
            for (N node : nodes) {
                switch (node.getClass().getSimpleName()) {
                    case "HtmlAnchor":
                        Observable.just((HtmlAnchor) node).subscribe(n -> {
                            String href = n.getHrefAttribute();
                            if (href.startsWith("http")) {
                                result.add((T) href);
                            } else {
                                result.add((T) (url.getProtocol() + "://" + url.getHost() + href));
                            }
                        });
                        break;
                    case "HtmlTable":
                        result.addAll((List<T>) ((HtmlTable) node)
                                .getBodies()
                                .stream()
                                .flatMap(b -> b.getRows().stream())
                                .map(r -> r.getCells().stream().map(c -> c.asText()).collect(Collectors.toList()))
//									.flatMap(r -> r.getCells().stream())
//									.map(c -> c.asText().trim())
                                .collect(Collectors.toList()));
                        break;
                    default:

                }

            }
            return result;
        }
    }

    /**
     * @author kuojian21
     */
    @Data
    public static class Tuple<X, Y> {
        private final X x;
        private final Y y;

        public Tuple(X x, Y v) {
            super();
            this.x = x;
            this.y = v;
        }

        public static <X, Y> Tuple<X, Y> tuple(X x, Y y) {
            return new Tuple<>(x, y);
        }
    }

}
