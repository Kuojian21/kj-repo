package com.kj.repo.infra.http;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.SocketConfig;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;
import com.kj.repo.infra.bean.BeanSupplier;

/**
 * @author kuojian21
 */
public class HttpCompBuilder {

    private static final Supplier<CloseableHttpClient> SYNC = new BeanSupplier<CloseableHttpClient>(() -> {
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setDefaultMaxPerRoute(20);
        connManager.setMaxTotal(200);
        connManager.setDefaultSocketConfig(SocketConfig.custom().setTcpNoDelay(true).setSoTimeout(10000).build());
        HttpClientBuilder builder = HttpClientBuilder.create().setConnectionManager(connManager)
                .setDefaultRequestConfig(
                        RequestConfig.custom().setConnectTimeout(10000).setConnectionRequestTimeout(10000).build());
        return builder.build();
    });

    private static final Supplier<CloseableHttpAsyncClient> ASYNC = new BeanSupplier<CloseableHttpAsyncClient>(() -> {
        try {
            PoolingNHttpClientConnectionManager connManager = new PoolingNHttpClientConnectionManager(
                    new DefaultConnectingIOReactor(IOReactorConfig.DEFAULT, Executors.defaultThreadFactory()));
            connManager.setDefaultMaxPerRoute(20);
            connManager.setMaxTotal(200);
            return HttpAsyncClientBuilder.create().setConnectionManager(connManager).build();
        } catch (IOReactorException e) {
            return null;
        }
    });
    private final String url;
    private final METHOD method;
    private List<Header> headers = Lists.newArrayList();
    private List<BasicNameValuePair> nameValuePairs = Lists.newArrayList();
    private BeanSupplier<HttpClientContext> context = new BeanSupplier<HttpClientContext>(() -> {
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(new BasicCookieStore());
        return context;
    });
    private String json;

    public HttpCompBuilder(String url, METHOD method) {
        super();
        this.url = url;
        this.method = method;
    }

    private static CloseableHttpClient sync() {
        return SYNC.get();
    }

    private static CloseableHttpAsyncClient async() {
        return ASYNC.get();
    }

    public static String toString(HttpEntity entity) throws ParseException, IOException {
        return EntityUtils.toString(entity, "UTF-8");
    }

    public static HttpCompBuilder get(String url) {
        return new HttpCompBuilder(url, METHOD.GET);
    }

    public static HttpCompBuilder post(String url) {
        return new HttpCompBuilder(url, METHOD.POST);
    }

    public HttpCompBuilder addHeader(String name, String value) {
        this.headers.add(new BasicHeader(name, value));
        return this;
    }

    public HttpCompBuilder addNameValuePair(String name, String value) {
        this.nameValuePairs.add(new BasicNameValuePair(name, value));
        return this;
    }

    public HttpCompBuilder setJson(String json) {
        this.json = json;
        return this;
    }

    public HttpCompBuilder addCookie(Cookie cookie) {
        this.context.get().getCookieStore().addCookie(cookie);
        return this;
    }

    public <T> T sync(Function<CloseableHttpResponse, T> func) throws Exception {
        CloseableHttpResponse response = null;
        try {
            response = HttpCompBuilder.sync().execute(this.buildRequest(), context.get());
            return func.apply(response);
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public Future<HttpResponse> async(FutureCallback<HttpResponse> callback) {
        return HttpCompBuilder.async().execute(this.buildRequest(), context.get(), callback);
    }

    private HttpUriRequest buildRequest() {
        HttpUriRequest request = null;
        switch (this.method) {
            case GET:
                request = new HttpGet(url);
                break;
            case POST:
                HttpPost t = new HttpPost(url);
                if (!CollectionUtils.isEmpty(this.nameValuePairs)) {
                    t.setEntity(new UrlEncodedFormEntity(this.nameValuePairs, Charset.forName("UTF-8")));
                } else if (Strings.isNotBlank(this.json)) {
                    StringEntity stringEntity = new StringEntity(json, "UTF-8");
                    stringEntity.setContentEncoding("UTF-8");
                    stringEntity.setContentType("application/json");
                    t.setEntity(stringEntity);
                }
                request = t;
                break;
            default:
        }
        return request;
    }

    public enum METHOD {
        GET, POST
    }

    @FunctionalInterface
    public static interface Function<T, R> {
        R apply(T t) throws Exception;
    }

}
