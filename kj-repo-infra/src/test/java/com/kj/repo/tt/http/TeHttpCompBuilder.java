package com.kj.repo.tt.http;

import com.kj.repo.infra.http.HttpCompBuilder;

/**
 * @author kuojian21
 */
public class TeHttpCompBuilder {

    public static void main(String[] args) throws Exception {
        System.out.println(HttpCompBuilder.get("https://www.lmlc.com").sync((r) -> {
            return HttpCompBuilder.toString(r.getEntity());
        }));
    }

}
