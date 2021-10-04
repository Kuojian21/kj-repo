package com.kj.repo.demo.xpath;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.curator.shaded.com.google.common.base.Joiner;

import com.gargoylesoftware.htmlunit.html.DomAttr;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.google.common.collect.Lists;

/**
 * @author kj
 */
public class HtmlunitXpath {
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
