package com.kj.repo.demo.xpath;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.seimicrawler.xpath.JXDocument;

/**
 * @author kj
 */
public class JsoupXpath {

    public static JXDocument xpath(String html) {
        JXDocument doc = JXDocument.create(html);
        return doc;
    }

    public static Document jsoup(String html) {
        return Jsoup.parse(html);
    }

}