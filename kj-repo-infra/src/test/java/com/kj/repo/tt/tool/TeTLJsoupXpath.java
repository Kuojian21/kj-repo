package com.kj.repo.tt.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.seimicrawler.xpath.JXNode;
import org.seimicrawler.xpath.exception.XpathSyntaxErrorException;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.kj.repo.infra.tool.TLJsoupXpath;

public class TeTLJsoupXpath {

    public static void git() throws FileNotFoundException, XpathSyntaxErrorException, IOException {
        String xpath = "//a[@class='project']/@href";
        Set<String> sets = Sets.newHashSet();
        for (File file : new File("/Users/kuojian21/kj/git/html").listFiles()) {
            List<Object> objs = TLJsoupXpath.xpath(Joiner.on("\n").join(Files.readAllLines(Paths.get(file.toURI()))))
                    .sel(xpath);
            for (Object obj : objs) {
                sets.add(obj.toString());
            }
        }
        for (String s : sets.stream().sorted().collect(Collectors.toList())) {
            System.out.println(s.substring(1));
        }
    }

    public static void m() throws FileNotFoundException, XpathSyntaxErrorException, IOException {
        String xpath = "//tr";
        Set<String> sets = Sets.newHashSet();
        File file = new File("/Users/kuojian21/kj/m.list");
        List<JXNode> objs = TLJsoupXpath.xpath(Joiner.on("\n").join(Files.readAllLines(Paths.get(file.toURI()))))
                .selN(xpath);
        for (JXNode obj : objs) {
            List<JXNode> o = obj.sel("//td");
            if (o.isEmpty()) {
                continue;
            }
            sets.add(o.get(0).asElement().textNodes().get(0).text().trim() + "/"
                    + o.get(1).asElement().textNodes().get(0).text().trim());
        }
        for (String s : sets.stream().sorted().collect(Collectors.toList())) {
            System.out.println(s);
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, XpathSyntaxErrorException {
        m();
    }

}
