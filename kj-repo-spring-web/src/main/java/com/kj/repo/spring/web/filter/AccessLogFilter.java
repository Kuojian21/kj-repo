package com.kj.repo.spring.web.filter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.gson.Gson;

/**
 * @author kuojian21
 */
public class AccessLogFilter implements Filter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Gson gson = new Gson();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String contentType = request.getContentType();
        HttpServletRequest req = (HttpServletRequest) request;
        StringBuilder params = new StringBuilder();
        if (Strings.isNullOrEmpty(contentType) || contentType.indexOf("application/json") < 0) {
            params.append(gson.toJson(request.getParameterMap()));
        } else {
            AccessLogServletRequestWrapper requestWrapper = new AccessLogServletRequestWrapper(req);
            BufferedReader br = requestWrapper.getReader();
            String line = null;
            while ((line = br.readLine()) != null) {
                params.append(line);
            }
            request = requestWrapper;
        }
        logger.info("AccessLog:{}:{}", req.getRequestURI(), params.toString());
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }

    /**
     * @author kj
     */
    public class AccessLogServletRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] body;

        public AccessLogServletRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            InputStream is = request.getInputStream();
            try {
                byte[] buffer = new byte[1024];
                while (true) {
                    int rtn = is.read(buffer);
                    if (rtn == -1) {
                        break;
                    }
                    os.write(buffer, 0, rtn);
                }
            } finally {
                is.close();
            }
            body = os.toByteArray();
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }

        @Override
        public ServletInputStream getInputStream() {
            final ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {

                @Override
                public int read() throws IOException {
                    return bais.read();
                }

                @Override
                public boolean isFinished() {
                    return bais.available() <= 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {

                }
            };
        }
    }

}
