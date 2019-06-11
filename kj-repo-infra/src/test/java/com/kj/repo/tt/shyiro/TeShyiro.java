package com.kj.repo.tt.shyiro;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.BinaryLogClient.LifecycleListener;
import com.google.gson.Gson;

/**
 * @author kj
 */
public class TeShyiro {

    private static Logger log = LoggerFactory.getLogger(TeShyiro.class);

    public static void main(String[] args) throws IOException {
        log.info("");
        BinaryLogClient client = new BinaryLogClient(args[0], Integer.parseInt(args[1]), args[2], args[3]);
        Gson gson = new Gson();
        client.registerEventListener(event -> log.info("{} {}", event.getData().getClass(), gson.toJson(event)));
        client.registerLifecycleListener(new LifecycleListener() {
            @Override
            public void onConnect(BinaryLogClient client) {
                log.info("");
            }

            @Override
            public void onCommunicationFailure(BinaryLogClient client, Exception ex) {
                log.info("", ex);

            }

            @Override
            public void onEventDeserializationFailure(BinaryLogClient client, Exception ex) {
                log.info("", ex);
            }

            @Override
            public void onDisconnect(BinaryLogClient client) {
                log.info("");
            }

        });
        client.connect();
    }


}
