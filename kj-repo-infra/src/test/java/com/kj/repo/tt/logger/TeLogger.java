package com.kj.repo.tt.logger;

import java.net.URL;
import java.util.ServiceLoader;
import java.util.stream.IntStream;

import org.apache.logging.log4j.spi.Provider;

import com.alibaba.fastjson.JSON;
import com.kj.repo.infra.logger.Log4j2Helper;
import com.kj.repo.infra.logger.LogbackHelper;
import com.kj.repo.infra.utils.JavaUtil;
import com.kj.repo.infra.utils.RunUtil;

import ch.qos.logback.classic.spi.Configurator;

//import ch.qos.logback.classic.spi.Configurator;

/**
 * @author kj
 * Created on 2020-03-15
 */
public class TeLogger {

    public static void main(String[] args) {
        TeLogger bean = new TeLogger();
        IntStream.range(0, 1).boxed().forEach(i -> bean.run());
    }

    public void run() {
        logback();
        //        log4j();
        //        relate();
    }

    public void logback() {
        LogbackHelper.syncLogger().info("{}", "sync-logback");
        LogbackHelper.asyncLogger().info("{}", "async-logback");

    }

    public void log4j() {
        Log4j2Helper.syncLogger().info("{}", "sync-log4j2");
        Log4j2Helper.asyncLogger().info("{}", "async-log4j2");
    }

    public void relate() {
        for (URL url : RunUtil.run(() -> JavaUtil.resources("log4j2.component.properties"))) {
            LogbackHelper.getLogger().info("{}", url.toString());
        }
        for (URL url : RunUtil.run(() -> JavaUtil.resources("org/slf4j/impl/StaticLoggerBinder.class"))) {
            LogbackHelper.getLogger().info("{}", url.toString());
        }
        for (Configurator configurator : JavaUtil.services(Configurator.class)) {
            LogbackHelper.getLogger()
                    .info("{} {}", configurator.getClass().getName(), JavaUtil.location(configurator.getClass()));
        }
        for (Provider provider : ServiceLoader.load(Provider.class)) {
            Log4j2Helper.getLogger()
                    .info("{} {} {}", provider.getClass().getName(),
                            provider.getClass().getProtectionDomain().getCodeSource().getLocation(),
                            JSON.toJSON(provider));
        }
    }
}
