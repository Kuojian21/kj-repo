package com.kj.repo.infra.tool;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.seimicrawler.xpath.JXDocument;

/**
 * @author kj
 */
public class TLJsoupXpath {

    public static JXDocument xpath(String html) {
        JXDocument doc = JXDocument.create(html);
        return doc;
    }

    public static Document jsoup(String html) {
        return Jsoup.parse(html);
    }

}