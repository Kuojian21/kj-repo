package com.kj.repo.tt.savor;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import com.kj.repo.infra.logger.LoggerHelper;
import com.mysql.cj.jdbc.Driver;

public class TeDeadlock {

    private static final Logger logger = LoggerHelper.getLogger();

    public static void main(String[] args) throws SQLException, InterruptedException {
        logger.info("");
        //		System.setProperty("socksProxyHost", "127.0.0.1");
        //		System.setProperty("socksProxyPort", "8088");
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(
                new SimpleDriverDataSource(new Driver(), args[0], args[1], args[2]));
        String sql = "insert into savor_base_test(hash_key,name) values(:hashKey,:name)";
        for (int i = 0; i < 100000; i++) {
            int x = i;
            CountDownLatch latch = new CountDownLatch(1);
            int len = 3;
            CountDownLatch latch2 = new CountDownLatch(len);
            for (int j = 0; j < len; j++) {
                new Thread(() -> {
                    //					while (true) {
                    try {
                        latch.await();
                        jdbcTemplate.update(sql, new MapSqlParameterSource().addValue("hashKey", "" + x + "")
                                .addValue("name", UUID.randomUUID().toString()));
                        //						logger.info(x + ":" + jdbcTemplate.update(sql, new
                        //						MapSqlParameterSource()
                        //								.addValue("hashKey", "" + x + "").addValue("name", UUID
                        //								.randomUUID().toString())));
                        latch2.countDown();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    } finally {
                        latch2.countDown();
                    }
                    //					}
                }).start();
            }
            //			new Thread(() -> {
            ////				while (true) {
            //					try {
            //						jdbcTemplate.update("delete from savor_base_test where hash_key = '" + x + "'",
            //								new MapSqlParameterSource());
            ////					logger.info("d:" + jdbcTemplate.update("delete from savor_base_test", new
            // MapSqlParameterSource()));
            //					} catch (Throwable t) {
            //						t.printStackTrace();
            //					}
            ////				}
            //			}).start();
            latch.countDown();
            latch2.await();
        }
    }

}
