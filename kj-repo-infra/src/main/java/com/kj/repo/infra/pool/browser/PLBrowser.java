package com.kj.repo.infra.pool.browser;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.kj.repo.infra.pool.base.PLBase;

/**
 * @author kuojian21 http://htmlunit.sourceforge.net/
 */
public class PLBrowser extends PLBase<WebClient> {

    public static PLBrowser DEFAULT = browser(BrowserVersion.CHROME);

    public PLBrowser(GenericObjectPool<WebClient> pool) {
        super(pool);
    }

    public static PLBrowser browser(BrowserVersion version) {
        return new PLBrowser(new GenericObjectPool<WebClient>(new BasePooledObjectFactory<WebClient>() {
            @Override
            public PooledObject<WebClient> wrap(WebClient webClient) {
                return new DefaultPooledObject<WebClient>(webClient);
            }

            @Override
            public WebClient create() throws Exception {
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
            }

            @Override
            public void destroyObject(final PooledObject<WebClient> obj) throws Exception {
                obj.getObject().close();
            }
        }));
    }
}
