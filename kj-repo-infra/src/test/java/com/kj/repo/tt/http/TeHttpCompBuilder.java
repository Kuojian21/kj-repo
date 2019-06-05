package com.kj.repo.tt.http;

import com.kj.repo.infra.http.HttpCompBuilder;

/**
 * @author kuojian21
 */
public class TeHttpCompBuilder {

    public static void main(String[] args) throws Exception {
        Object obj = HttpCompBuilder.get("https://www.lmlc.com").sync((r) -> HttpCompBuilder.toString(r.getEntity()));
        System.out.println(obj);
    }

}
