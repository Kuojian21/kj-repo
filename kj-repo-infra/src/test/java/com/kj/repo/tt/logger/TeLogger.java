package com.kj.repo.tt.logger;

import java.net.URL;
import java.util.ServiceLoader;
import java.util.stream.IntStream;

import org.apache.logging.log4j.spi.Provider;

import com.alibaba.fastjson.JSON;
import com.kj.repo.infra.helper.JavaHelper;
import com.kj.repo.infra.helper.RunHelper;
import com.kj.repo.infra.logger.Log4jHelper;
import com.kj.repo.infra.logger.Slf4jHelper;

//import ch.qos.logback.classic.spi.Configurator;

/**
 * @author kj
 * Created on 2020-03-15
 */
public class TeLogger {

    public static void main(String[] args) {
        TeLogger bean = new TeLogger();
        IntStream.range(0, 3).boxed().forEach(i -> bean.run());
    }

    public void run() {
        log4j();
        slf4j();
    }

    public void slf4j() {
        Slf4jHelper.getLogger().info("{}", "run0");
        for (URL url : RunHelper.run(() -> JavaHelper.resources("org/slf4j/impl/StaticLoggerBinder.class"))) {
            Slf4jHelper.getLogger().info("{}", url.toString());
        }
        //        for (Configurator configurator : JavaHelper.services(Configurator.class)) {
        //            Slf4jHelper.getLogger()
        //                    .info("{} {}", configurator.getClass().getName(), JavaHelper.location(configurator
        //                    .getClass()));
        //        }
    }

    public void log4j() {
        Log4jHelper.getLogger().info("{}", "run0");
        for (Provider provider : ServiceLoader.load(Provider.class)) {
            Log4jHelper.getLogger()
                    .info("{} {} {}", provider.getClass().getName(),
                            provider.getClass().getProtectionDomain().getCodeSource().getLocation(),
                            JSON.toJSON(provider));
        }
    }
}
