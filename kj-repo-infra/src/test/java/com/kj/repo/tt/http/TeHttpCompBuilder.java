package com.kj.repo.tt.http;

import java.net.URL;
import java.util.Set;

import org.apache.http.impl.cookie.BasicClientCookie;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.beust.jcommander.internal.Sets;
import com.kj.repo.infra.http.HttpCompBuilder;

/**
 * @author kuojian21
 */
public class TeHttpCompBuilder {

    public static void main(String[] args) throws Exception {
//		Object obj = HttpCompBuilder.get("https://www.lmlc.com").sync((r) -> HttpCompBuilder.toString(r.getEntity()));
//		System.out.println(obj);
        git(args);
    }

    public static Set<String> git(String args[]) throws Exception {
        BasicClientCookie cookie1 = new BasicClientCookie("_gitlab_session", args[1]);
        BasicClientCookie cookie2 = new BasicClientCookie("sidebar_collapsed", "false");
        URL url = new URL(args[0]);
        cookie1.setDomain(url.getHost());
        cookie1.setPath("/");
        cookie2.setDomain(url.getHost());
        cookie2.setPath("/");
        Set<String> result = Sets.newHashSet();
        JSON.parseArray(HttpCompBuilder.get(args[0] + "dashboard/groups.json").addCookie(cookie1).addCookie(cookie2)
                .sync((r) -> HttpCompBuilder.toString(r.getEntity()))).forEach(a -> {
            try {
                JSONObject jsonObject = (JSONObject) a;
                String name = jsonObject.getString("name");
                Integer page = 1;
                do {
                    System.out.println(args[0] + "groups/" + name + "/-/children.json?page=" + page);
                    JSONArray jsonArray = JSONArray.parseArray(
                            HttpCompBuilder.get(args[0] + "groups/" + name + "/-/children.json?page=" + page)
                                    .addCookie(cookie1).addCookie(cookie2)
                                    .sync((r) -> HttpCompBuilder.toString(r.getEntity())));
                    Set<String> t = Sets.newHashSet();
                    jsonArray.forEach(p -> {
                        String pro = ((JSONObject) p).getString("relative_path");
                        t.add(pro);
                    });
                    result.addAll(t);
                    page++;
                    if (t.size() <= 1) {
                        return;
                    }
                } while (true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        System.out.println(result.size());
        return result;
    }

}
