package com.kj.repo.tt.logger;

import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author kj
 * Created on 2020-03-15
 */
public class TeLogger {

    public static void main(String[] args) {
        TeLogger bean = new TeLogger();
        IntStream.range(0, 3).boxed().forEach(i -> bean.run());
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public void run() {
        logger.info("{}", "run");
    }
}
