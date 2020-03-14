package com.kj.repo.tt.helper;

import java.sql.SQLException;

import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import com.kj.repo.infra.helper.SavorHelper;
import com.mysql.cj.jdbc.Driver;

/**
 * @author kj
 */
public class TeSavorHelper {

    public static void main(String[] args) throws SQLException {
        /*
         * -DsocksProxyHost= -DsocksProxyPort=8088
         */
        //		System.setProperty("socksProxyHost", "127.0.0.1");
        //		System.setProperty("socksProxyPort", "8088");
        /**
         * <pre>
         *     		create table savor_test(
         * 						id bigint(20) unsigned not null primary key comment '自增主键',
         * 						hash_key varchar(64) comment 'key',
         * 						value varchar(128)
         * 						comment 'value',
         * 						name varchar(64) comment 'name',
         * 						sex varchar(64) comment 'sex',
         * 						age tinyint comment 'age',
         * 						create_time bigint(20) comment '创建时间',
         * 						update_time timestamp default now() comment '创建时间'
         * 					)ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;
         * </pre>
         */
        SavorHelper.Model model = SavorHelper.mysql(new SimpleDriverDataSource(new Driver(), args[0], args[1], args[2]),
                args[3]);
        SavorHelper.code(model);
    }

}
