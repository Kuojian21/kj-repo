package com.kj.repo.infra.browser;

import org.apache.commons.pool2.impl.GenericObjectPool;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.kj.repo.infra.base.pool.Pool;
import com.kj.repo.infra.helper.GenericPoolHelper;

/**
 * @author kj http://htmlunit.sourceforge.net/
 */
public class Browser extends Pool<WebClient> {

    public Browser(GenericObjectPool<WebClient> pool) {
        super(pool);
    }

    public static Browser browser(BrowserVersion version) {
        return new Browser(GenericPoolHelper.wrap(() -> {
            WebClient webClient = new WebClient(version);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setActiveXNative(true);
            webClient.getOptions().setCssEnabled(true);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setRedirectEnabled(true);
            webClient.getOptions().setActiveXNative(true);
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            return webClient;
        }, WebClient::close));
    }
}
