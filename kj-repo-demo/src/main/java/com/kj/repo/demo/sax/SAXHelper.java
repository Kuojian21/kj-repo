package com.kj.repo.demo.sax;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author kj
 * Created on 2020-03-31
 */
public class SAXHelper {

    public static void parse(InputStream is)
            throws IOException, SAXException, ParserConfigurationException {
        parse(is, new DefaultHandler() {
            @Override
            public void startDocument() throws SAXException {
                super.startDocument();
            }

            @Override
            public void endDocument() throws SAXException {
                super.endDocument();
            }

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes)
                    throws SAXException {
                super.startElement(uri, localName, qName, attributes);
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                super.characters(ch, start, length);
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                super.endElement(uri, localName, qName);
            }
        });
    }

    public static void parse(InputStream is, DefaultHandler db)
            throws IOException, SAXException, ParserConfigurationException {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setValidating(true);
            SAXParser parser = spf.newSAXParser();
            parser.parse(is, db);
        } finally {
            is.close();
        }

    }
}
