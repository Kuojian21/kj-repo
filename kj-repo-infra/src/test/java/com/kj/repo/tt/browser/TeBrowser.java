package com.kj.repo.tt.browser;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.curator.shaded.com.google.common.base.Joiner;
import org.slf4j.Logger;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.html.DomAttr;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.kj.repo.infra.browser.Browser;
import com.kj.repo.infra.logger.LoggerHelper;
import com.kj.repo.tt.net.http.TeHttpCompBuilder;

public class TeBrowser {

    private static final Logger logger = LoggerHelper.getLogger();

    public static void main(String[] args) throws Exception {
        //        System.setProperty("socksProxyHost", "127.0.0.1");
        //        System.setProperty("socksProxyPort", "8088");
        //        gatherproxy(args);
        Browser browser = Browser.browser(BrowserVersion.CHROME);
        Set<String> rtn = Sets.newHashSet(git(browser, args));
        rtn.addAll(TeHttpCompBuilder.git(args));
        System.out.println(rtn.size());
        rtn.stream().sorted().forEach(System.out::println);
    }

    public static List<String> git(Browser browser, String[] args) throws Exception {
        List<String> rtn = Lists.newArrayList();
        URL domain = new URL(args[0]);
        browser.execute(t -> {
            t.addCookie("_gitlab_session=" + args[1], domain, null);
            t.addCookie("sidebar_collapsed=false", domain, null);
            int pageIndex = 1;
            int pageCount = 0;
            do {
                HtmlPage page = t
                        .getPage(args[0] + "?non_archived=true&page=" + pageIndex + "&sort=latest_activity_desc");
                List<String> p = parse(page, "//a[@class='project']/@href");
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

    public static List<String> gitIDC(Browser browser, String[] args) throws Exception {
        List<String> rtn = Lists.newArrayList();
        URL domain = new URL(args[0]);
        browser.execute(t -> {
            t.addCookie("_gitlab_session=" + args[1], domain, null);
            t.addCookie("sidebar_collapsed=false", domain, null);
            int pageIndex = 1;
            int pageCount = 0;
            do {
                HtmlPage page = t
                        .getPage(args[0] + "/explore/projects?page=" + pageIndex);
                List<String> p = parse(page, "//a[@class='project']/@href");
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

    public static void gatherproxy(Browser browser, String[] arg) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
        List<String> urls = browser.execute(w -> {
            HtmlPage page = w.getPage("http://www.gatherproxy.com/sockslist/sockslistbycountry");
            return parse(page, "//ul[@class='pc-list']/li/a");
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
                    browser.execute(w -> {
                        List<String> rows = parse(w.getPage(url),
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
    
    public static <N> List<String> parse(HtmlPage page, String xpath) {
        List<String> result = Lists.newArrayList();
        List<N> nodes = page.getByXPath(xpath);
        URL baseURL = page.getBaseURL();
        for (N node : nodes) {
            switch (node.getClass().getSimpleName()) {
                case "HtmlAnchor":
                    String href = ((HtmlAnchor) node).getHrefAttribute();
                    if (href.startsWith("http")) {
                        result.add(href);
                    } else {
                        result.add(baseURL.getProtocol() + "://" + baseURL.getHost() + href);
                    }
                    break;
                case "HtmlTable":
                    result.addAll(((HtmlTable) node).getBodies().stream().flatMap(b -> b.getRows().stream())
                            .map(r -> Joiner.on(",")
                                    .join(r.getCells().stream().map(c -> c.asText()).collect(Collectors.toList())))
                            .collect(Collectors.toList()));
                    break;
                case "DomAttr":
                    result.add(((DomAttr) node).getTextContent());
                default:

            }
        }
        return result;
    }

}
