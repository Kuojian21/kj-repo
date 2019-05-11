package com.kj.repo.tt.pool;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.kj.repo.infra.pool.PLBrowser;

public class TePLBrowser {

    public static Logger logger = LoggerFactory.getLogger(TePLBrowser.class);

    public static PLBrowser<WebClient> kjBrowser = PLBrowser.browser(BrowserVersion.CHROME);

    public static void git(String[] args) throws Exception {
        URL domain = new URL(args[0]);
        kjBrowser.execute(t -> {
            try {
                t.addCookie("_did=web_966972270286159", domain, null);
                t.addCookie("_gitlab_session=d44e489f50cd9710f4a014130ca79f17", domain, null);
                t.addCookie("sidebar_collapsed=false", domain, null);
                HtmlPage page = t.getPage(args[0]);
                List<String> all = PLBrowser.Helper.parse(page, "//a[@class='project']/@href");
                do {
                    List<HtmlAnchor> anchors = page.getByXPath("//li[@class='next']/a");
                    if (anchors.isEmpty()) {
                        System.out.println(all.size());
                        all.stream().sorted().forEach(System.out::println);
                        break;
                    }
                    page = anchors.get(0).click();
                    all.addAll(PLBrowser.Helper.parse(page, "//a[@class='project']/@href"));
                } while (true);
            } catch (Exception e) {
                logger.error("", e);
            }
        });
    }

    public static void gatherproxy(String[] arg) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
        List<String> urls = kjBrowser.execute(w -> {
            HtmlPage page = w.getPage("http://www.gatherproxy.com/sockslist/sockslistbycountry");
            return PLBrowser.Helper.parse(page, "//ul[@class='pc-list']/li/a");
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
                    kjBrowser.execute(w -> {
                        List<List<String>> rows = PLBrowser.Helper.parse(w.getPage(url),
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

    public static void main(String[] args) throws Exception {
        System.setProperty("socksProxyHost", "127.0.0.1");
        System.setProperty("socksProxyPort", "8088");
        gatherproxy(args);

        kjBrowser.shutdown();
    }

}
