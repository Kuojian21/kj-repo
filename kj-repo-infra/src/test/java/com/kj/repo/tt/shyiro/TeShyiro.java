package com.kj.repo.tt.shyiro;

import java.io.IOException;

import org.slf4j.Logger;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.BinaryLogClient.LifecycleListener;
import com.google.gson.Gson;
import com.kj.repo.infra.logger.LoggerHelper;

/**
 * @author kj
 */
public class TeShyiro {

    private static final Logger logger = LoggerHelper.getLogger();

    public static void main(String[] args) throws IOException {
        logger.info("");
        BinaryLogClient client = new BinaryLogClient(args[0], Integer.parseInt(args[1]), args[2], args[3]);
        Gson gson = new Gson();
        client.registerEventListener(event -> logger.info("{} {}", event.getData().getClass(), gson.toJson(event)));
        client.registerLifecycleListener(new LifecycleListener() {
            @Override
            public void onConnect(BinaryLogClient client) {
                logger.info("");
            }

            @Override
            public void onCommunicationFailure(BinaryLogClient client, Exception ex) {
                logger.info("", ex);

            }

            @Override
            public void onEventDeserializationFailure(BinaryLogClient client, Exception ex) {
                logger.info("", ex);
            }

            @Override
            public void onDisconnect(BinaryLogClient client) {
                logger.info("");
            }

        });
        client.connect();
    }


}
