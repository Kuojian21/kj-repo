package com.kj.repo.tt.pool;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.kj.repo.infra.pool.browser.PLBrowser;
import com.kj.repo.infra.pool.browser.PLBrowserHelper;
import com.kj.repo.tt.http.TeHttpCompBuilder;

public class TePLBrowser {

    public static Logger logger = LoggerFactory.getLogger(TePLBrowser.class);

    public static void main(String[] args) throws Exception {
//        System.setProperty("socksProxyHost", "127.0.0.1");
//        System.setProperty("socksProxyPort", "8088");
//        gatherproxy(args);
        Set<String> rtn = Sets.newHashSet(git(args));

        rtn.addAll(TeHttpCompBuilder.git(args));
        System.out.println(rtn.size());
        rtn.stream().sorted().forEach(System.out::println);
    }

    public static List<String> git(String[] args) throws Exception {
        List<String> rtn = Lists.newArrayList();
        URL domain = new URL(args[0]);
        PLBrowser.DEFAULT.execute(t -> {
            t.addCookie("_gitlab_session=" + args[1], domain, null);
            t.addCookie("sidebar_collapsed=false", domain, null);
            int pageIndex = 1;
            int pageCount = 0;
            do {
                HtmlPage page = t
                        .getPage(args[0] + "?non_archived=true&page=" + pageIndex + "&sort=latest_activity_desc");
                List<String> p = PLBrowserHelper.parse(page, "//a[@class='project']/@href");
                rtn.addAll(p);
                logger.info("pageIndex:{} pageSize:{}", pageIndex, p.size());
                if (p.size() < pageCount || p.size() == 0) {
                    break;
                }
                pageCount = p.size();
                pageIndex++;
            } while (true);
        });
        return rtn;
    }

    public static void gatherproxy(String[] arg) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
        List<String> urls = PLBrowser.DEFAULT.execute(w -> {
            HtmlPage page = w.getPage("http://www.gatherproxy.com/sockslist/sockslistbycountry");
            return PLBrowserHelper.parse(page, "//ul[@class='pc-list']/li/a");
        });

        String path = Paths.get(System.getProperty("user.home"), "kj", "crawler", "gatherproxy").toFile()
                .getAbsolutePath();
        if (!Paths.get(path).toFile().exists()) {
            Paths.get(path).toFile().mkdirs();
        }

        CountDownLatch latch = new CountDownLatch(urls.size());

        for (String url : urls) {
            executor.submit(() -> {
                try {
                    logger.info("{}", url);
                    PLBrowser.DEFAULT.execute(w -> {
                        List<String> rows = PLBrowserHelper.parse(w.getPage(url),
                                "//div[@class='proxy-list']/table[@id='tblproxy']");
                        String file = url.substring(url.lastIndexOf("=") + 1);
                        if (Paths.get(path, file).toFile().exists()) {
                            Paths.get(path, file).toFile().delete();
                        }
                        Files.createFile(Paths.get(path, file));
                        Files.write(Paths.get(path, file),
                                rows.stream().map(r -> r.toString()).collect(Collectors.toList()));
                    });
                } catch (Exception e) {
                    logger.error("", e);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

    }

}
