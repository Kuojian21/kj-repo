package com.kj.repo.demo.retrofit;

import java.util.concurrent.TimeUnit;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import retrofit2.adapter.guava.GuavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.jaxb.JaxbConverterFactory;
import retrofit2.converter.protobuf.ProtoConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * @author kj
 */
public class RetrofitBuilder {

    public static OkHttpClient okHttp() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(100);
        dispatcher.setMaxRequestsPerHost(100);
        OkHttpClient.Builder builder = new OkHttpClient.Builder().dispatcher(dispatcher)
                .connectTimeout(100, TimeUnit.MILLISECONDS).readTimeout(100, TimeUnit.MILLISECONDS)
                .writeTimeout(100, TimeUnit.MILLISECONDS);
        return builder.build();
    }

    public static <T> T build(String url, Class<T> iface) {
        return new retrofit2.Retrofit.Builder().baseUrl(url).addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(ProtoConverterFactory.create()).addConverterFactory(JaxbConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .addCallAdapterFactory(GuavaCallAdapterFactory.create()).client(okHttp()).build().create(iface);
    }

}
