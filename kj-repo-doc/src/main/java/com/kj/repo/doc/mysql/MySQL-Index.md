# MySQL的N次踩坑经验总结-Index

### 0.本篇测试环境
- 数据库版本
    ```
    5.7.26-29-log Percona Server (GPL), Release 29, Revision 11ad961       
    ```
- 表结构
    - mark:审核中台真实表结构，非真实字段名称，数据量1千万。   
    - uid:审核中台唯一ID,每审核单元唯一。
    - bid:业务方ID。
    - dimen1/dimen2:维度,代表审核单元分类，eg:审核单元的桶、机器分类等等,dimen1范围0~99，dimen2范围0~3。
    - priority:审核单元优先级，一般为进审时间，这里是插入时生成的随机数。
    - status:审核单元状态，0代表初始状态，1代表准备状态，2代表完结状态
    ```
    CREATE TABLE `mark` (
      `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
      `uid` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '中台唯一ID',
      `bid` varchar(128) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '业务方ID',
      `status` tinyint(3) unsigned NOT NULL DEFAULT '0' COMMENT '数据状态',
      `action` int(11) NOT NULL DEFAULT '0' COMMENT '结果',
      `priority` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '优先级:排序字段',
      `dimen1` tinyint(3) unsigned NOT NULL DEFAULT '0' COMMENT '维度1',
      `dimen2` tinyint(3) unsigned NOT NULL DEFAULT '0' COMMENT '维度2',
      `create_time` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '添加时间',
      `update_time` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '修改时间',
      PRIMARY KEY (`id`),
      UNIQUE KEY `uidx_uid` (`uid`),
      KEY `idx_bid` (`bid`),
      KEY `idx_status_dimen12_priority` (`status`,`dimen1`,`dimen2`,`priority`),
      KEY `idx_status_dimen12_ctime` (`status`,`dimen1`,`dimen2`,`create_time`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Mark:标记';
    optimize table mark;
    analyze table mark;
    ```

### 1.status只有3个枚举值，适合创建索引吗？
- 分析

    随着时间推移，Mark数据记录在status上分布极不均匀，其中【status=2(表示状态已终结)】的记录占99%，而且业务的查询需求只有status=0或1，所以应该在status上创建索引。
    
### 2.怎么查询某业务的所有维度(dimen1/dimen2)?
- 方案一:覆盖索引
    - distinct
        - SQL 耗时:8.45 sec
         ```
         select distinct dimen1,dimen2 from mark;
         ```
      
        - 执行计划
        ```
        +----+-------------+-------+------------+-------+-----------------------------+-----------------------------+---------+------+----------+----------+------------------------------+
        | id | select_type | table | partitions | type  | possible_keys               | key                         | key_len | ref  | rows     | filtered | Extra                        |
        +----+-------------+-------+------------+-------+-----------------------------+-----------------------------+---------+------+----------+----------+------------------------------+
        |  1 | SIMPLE      | mark  | NULL       | index | idx_status_dimen12_priority | idx_status_dimen12_priority | 11      | NULL | 10718852 |   100.00 | Using index; Using temporary |
        +----+-------------+-------+------------+-------+-----------------------------+-----------------------------+---------+------+----------+----------+------------------------------+
        ```
      
        - trace:Using temporary
        ```
        "creating_tmp_table": {
            "tmp_table_info": {
                "table": "intermediate_tmp_table",
                "row_length": 3,
                "key_length": 2,
                "unique_constraint": false,
                "location": "memory (heap)",
                "row_limit_estimate": 5592405
            }
        }    
        ```

    - group-by
        - SQL 耗时:8.45 sec
        ```
        select dimen1,dimen2 from mark group by dimen1,dimen2;
        ```
      
        - 执行计划
        ```
        +----+-------------+-------+------------+-------+-----------------------------+-----------------------------+---------+------+----------+----------+----------------------------------------------+
        | id | select_type | table | partitions | type  | possible_keys               | key                         | key_len | ref  | rows     | filtered | Extra                                        |
        +----+-------------+-------+------------+-------+-----------------------------+-----------------------------+---------+------+----------+----------+----------------------------------------------+
        |  1 | SIMPLE      | mark  | NULL       | index | idx_status_dimen12_priority | idx_status_dimen12_priority | 11      | NULL | 10718852 |   100.00 | Using index; Using temporary; Using filesort |
        +----+-------------+-------+------------+-------+-----------------------------+-----------------------------+---------+------+----------+----------+----------------------------------------------+
      
        ```
        - trace:Using temporary; Using filesort
        ```
        "creating_tmp_table": {
          "tmp_table_info": {
            "table": "intermediate_tmp_table",
             "row_length": 3,
             "key_length": 2,
             "unique_constraint": false,
             "location": "memory (heap)",
             "row_limit_estimate": 5592405
          }
        }
        "filesort_summary": {
            "rows": 400,
            "examined_rows": 400,
            "number_of_tmp_files": 0,
            "sort_buffer_size": 7384,
            "sort_mode": "<sort_key, rowid>"
        }   
        ```
      
- 方案二:[Loose Index Scan](https://dev.mysql.com/doc/refman/8.0/en/group-by-optimization.html#loose-index-scan)
    - distinct 
        - SQL 耗时:0.00 sec
        ```
        select distinct dimen1,dimen2 from (select distinct status,dimen1,dimen2 from mark) as t;
        ```
      
        - 执行计划
        ```
        +----+-------------+------------+------------+-------+-----------------------------+-----------------------------+---------+------+-------+----------+--------------------------+
        | id | select_type | table      | partitions | type  | possible_keys               | key                         | key_len | ref  | rows  | filtered | Extra                    |
        +----+-------------+------------+------------+-------+-----------------------------+-----------------------------+---------+------+-------+----------+--------------------------+
        |  1 | PRIMARY     | <derived2> | NULL       | ALL   | NULL                        | NULL                        | NULL    | NULL | 27166 |   100.00 | Using temporary          |
        |  2 | DERIVED     | mark       | NULL       | range | idx_status_dimen12_priority | idx_status_dimen12_priority | 3       | NULL | 27166 |   100.00 | Using index for group-by |
        +----+-------------+------------+------------+-------+-----------------------------+-----------------------------+---------+------+-------+----------+--------------------------+
        ```
      
        - trace:Using temporary
        ```                      
        "creating_tmp_table": {
          "tmp_table_info": {
            "table": " `t`",
            "row_length": 4,
            "key_length": 0,
            "unique_constraint": false,
            "location": "memory (heap)",
            "row_limit_estimate": 4194304
          }
        }
        "creating_tmp_table": {
          "tmp_table_info": {
            "table": "intermediate_tmp_table",
            "row_length": 3,
            "key_length": 2,
            "unique_constraint": false,
            "location": "memory (heap)",
            "row_limit_estimate": 5592405
          }
        }      
        ```
      
    - group-by
        - SQL 耗时:0.00 sec
        ```
        select distinct dimen1,dimen2 from (select status,dimen1,dimen2 from mark group by status,dimen1,dimen2) as t;
        ```
      
        - 执行计划
        ```
        +----+-------------+------------+------------+-------+-----------------------------+-----------------------------+---------+------+-------+----------+--------------------------+
        | id | select_type | table      | partitions | type  | possible_keys               | key                         | key_len | ref  | rows  | filtered | Extra                    |
        +----+-------------+------------+------------+-------+-----------------------------+-----------------------------+---------+------+-------+----------+--------------------------+
        |  1 | PRIMARY     | <derived2> | NULL       | ALL   | NULL                        | NULL                        | NULL    | NULL | 27166 |   100.00 | Using temporary          |
        |  2 | DERIVED     | mark       | NULL       | range | idx_status_dimen12_priority | idx_status_dimen12_priority | 3       | NULL | 27166 |   100.00 | Using index for group-by |
        +----+-------------+------------+------------+-------+-----------------------------+-----------------------------+---------+------+-------+----------+--------------------------+
        ```
      
        - trace:Using temporary
        ```
        "creating_tmp_table": {
          "tmp_table_info": {
            "table": " `t`",
            "row_length": 4,
            "key_length": 0,
            "unique_constraint": false,
            "location": "memory (heap)",
            "row_limit_estimate": 4194304
          }
        }

        "creating_tmp_table": {
          "tmp_table_info": {
            "table": "intermediate_tmp_table",
            "row_length": 3,
            "key_length": 2,
            "unique_constraint": false,
            "location": "memory (heap)",
            "row_limit_estimate": 5592405
          }
        }                    
        ```
      
- 实验:[Loose Index Scan](https://dev.mysql.com/doc/refman/8.0/en/group-by-optimization.html#loose-index-scan) 索引顺序
    - distinct 
        - SQL 耗时:0.00 sec
        ```
        select distinct dimen2,dimen1 from (select status,dimen2,dimen1 from mark group by status,dimen1,dimen2) as t;
        ```
      
        - 执行计划
        ```
        +----+-------------+------------+------------+-------+-----------------------------+-----------------------------+---------+------+-------+----------+--------------------------+
        | id | select_type | table      | partitions | type  | possible_keys               | key                         | key_len | ref  | rows  | filtered | Extra                    |
        +----+-------------+------------+------------+-------+-----------------------------+-----------------------------+---------+------+-------+----------+--------------------------+
        |  1 | PRIMARY     | <derived2> | NULL       | ALL   | NULL                        | NULL                        | NULL    | NULL | 27166 |   100.00 | Using temporary          |
        |  2 | DERIVED     | mark       | NULL       | range | idx_status_dimen12_priority | idx_status_dimen12_priority | 3       | NULL | 27166 |   100.00 | Using index for group-by |
        +----+-------------+------------+------------+-------+-----------------------------+-----------------------------+---------+------+-------+----------+--------------------------+
        ```
      
        - trace:Using temporary
        ```
        "creating_tmp_table": {
          "tmp_table_info": {
            "table": " `t`",
            "row_length": 4,
            "key_length": 0,
            "unique_constraint": false,
            "location": "memory (heap)",
            "row_limit_estimate": 4194304
          }
        }
        "creating_tmp_table": {
          "tmp_table_info": {
            "table": "intermediate_tmp_table",
            "row_length": 3,
            "key_length": 2,
            "unique_constraint": false,
            "location": "memory (heap)",
            "row_limit_estimate": 5592405
          }
        }      
        ```
      
    - group-by 
        - SQL 耗时:8.89 sec
        ```
        select distinct dimen2,dimen1 from (select status,dimen2,dimen1 from mark group by status,dimen2,dimen1) as t
        ```
        - 执行计划
        ```
        +----+-------------+------------+------------+-------+-----------------------------+-----------------------------+---------+------+----------+----------+----------------------------------------------+
        | id | select_type | table      | partitions | type  | possible_keys               | key                         | key_len | ref  | rows     | filtered | Extra                                        |
        +----+-------------+------------+------------+-------+-----------------------------+-----------------------------+---------+------+----------+----------+----------------------------------------------+
        |  1 | PRIMARY     | <derived2> | NULL       | ALL   | NULL                        | NULL                        | NULL    | NULL | 10891042 |   100.00 | Using temporary                              |
        |  2 | DERIVED     | mark       | NULL       | index | idx_status_dimen12_priority | idx_status_dimen12_priority | 11      | NULL | 10891042 |   100.00 | Using index; Using temporary; Using filesort |
        +----+-------------+------------+------------+-------+-----------------------------+-----------------------------+---------+------+----------+----------+----------------------------------------------+
        ```
      
        - trace:Using temporary; Using filesort
        ```
        "creating_tmp_table": {
            "tmp_table_info": {
              "table": "intermediate_tmp_table",
              "row_length": 4,
              "key_length": 3,
              "unique_constraint": false,
              "location": "memory (heap)",
              "row_limit_estimate": 4194304
            }
        }
        "filesort_summary": {
            "rows": 120,
            "examined_rows": 120,
            "number_of_tmp_files": 0,
            "sort_buffer_size": 2472,
            "sort_mode": "<sort_key, rowid>"
        }
        "creating_tmp_table": {
          "tmp_table_info": {
            "table": "intermediate_tmp_table",
            "row_length": 3,
            "key_length": 2,
            "unique_constraint": false,
            "location": "memory (heap)",
            "row_limit_estimate": 5592405
          }
        }            
        ```
      
- 总结&分析

    - [Loose Index Scan](https://dev.mysql.com/doc/refman/8.0/en/group-by-optimization.html#loose-index-scan) 使用了搜索、定位算法来优化查询，比普通的【Using Index】索引扫描效率更高，其标志是执行计划中包含【Using index for group-by】。
    - 在满足索引前缀规则的前提下，group-by语句中字段顺序对算法的选择有影响，严格按照索引的真实顺序可选择的算法更多。对于distinct可先按照索引真实顺序来执行，再用临时表转换结果，所以几乎无影响。
    - 一定要满足前缀索引规则吗？[Index skip scan](https://dev.mysql.com/doc/refman/8.0/en/range-optimization.html#range-access-skip-scan) 一种神奇的扫描算法，在Oracle、MySQL8.0.13中都有支持，其原理是直接跳过索引前缀，eg:[where demon1 = 1]会被优化为[where (status,dimen1) in((0,1),(1,1),(2,1))]的等价方式。

### 3.Using temporary & Using filesort 就意味着效率低吗?
- Using temporary

    临时表分为内存临时表和磁盘临时表，trace中的【tmp_table_info】->【"location":"memory (heap)"】指明临时表类型；另外对于temporary的效率，即使是内存临时表，也要考虑数据量。传言：在MySQL中最好不要使用子查询，因为子查询会建立临时表，进而影响效率，其实说的是磁盘临时表，子查询适当时候还是可以用的。
    
- Using filesort

    与临时表类似，filesort也分在内存执行和磁盘执行，trace的【"filesort_summary"】->【"number_of_tmp_files": 0】表明排序使用的磁盘文件数量。对于filesort的效率，即使是在内存执行，也要考虑数据量。
        
### 4.一次查询可以使用多个索引吗？
- 添加索引
    ```
    alter table mark add index idx_dimen1(dimen1),add index idx_dimen2(dimen2);
    ```
  
- explain select * from mark where dimen1 = 1 and dimen2 = 2;
    ```
    +----+-------------+-------+------------+-------------+-----------------------+-----------------------+---------+------+--------+----------+-----------------------------------------------------+
    | id | select_type | table | partitions | type        | possible_keys         | key                   | key_len | ref  | rows   | filtered | Extra                                               |
    +----+-------------+-------+------------+-------------+-----------------------+-----------------------+---------+------+--------+----------+-----------------------------------------------------+
    |  1 | SIMPLE      | mark  | NULL       | index_merge | idx_dimen1,idx_dimen2 | idx_dimen1,idx_dimen2 | 1,1     | NULL | 109443 |   100.00 | Using intersect(idx_dimen1,idx_dimen2); Using where |
    +----+-------------+-------+------------+-------------+-----------------------+-----------------------+---------+------+--------+----------+-----------------------------------------------------+
    ```
  
- 总结&分析
    - 执行计划中的【key:idx_dimen1,idx_dimen2】和【Extra:Using intersect(idx_dimen1,idx_dimen2)】表明使用了[Index Merge](https://dev.mysql.com/doc/refman/8.0/en/index-merge-optimization.html) 。
    
### 5.索引中值相同的记录是按照主键排序的吗？
- 添加索引
    ```
    alter table mark add index idx_dimen1(dimen1);
    ```
- explain select * from mark order by dimen1,id limit 10;
    ```
    +----+-------------+-------+------------+-------+---------------+------------+---------+------+------+----------+-------+
    | id | select_type | table | partitions | type  | possible_keys | key        | key_len | ref  | rows | filtered | Extra |
    +----+-------------+-------+------------+-------+---------------+------------+---------+------+------+----------+-------+
    |  1 | SIMPLE      | mark  | NULL       | index | NULL          | idx_dimen1 | 1       | NULL |   10 |   100.00 | NULL  |
    +----+-------------+-------+------------+-------+---------------+------------+---------+------+------+----------+-------+
    ```
- 总结&分析
    - 反证法：假设不按照主键排序，则上面执行计划中的extra必定包含【Using filesort】，所以假设不成立。

### 6.单dimen数据查询应该如何设计？
- 查询语句
    ```
    select * from mark where status=1 and dimen1 = 90 and dimen2 = 1 and create_time <= unix_timestamp(now())*1000 - 120000 order by priority limit 1000;
    ```
    说明:这个SQL是为了按优先级(priority)查询【dimen1 = 1 and dimen2 = 90】的准备态数据，【create_time <= unix_timestamp(now())*1000 - 120000】的目的是为了给机器识别留时间。 
  
- 方案一:优先走排序索引(idx_status_dimen12_priority)
    - SQL 耗时:0.01 sec
    ```
    select * from mark force index(idx_status_dimen12_priority) where status=1 and dimen1 = 90 and dimen2 = 1 and create_time <= unix_timestamp(now())*1000 - 120000 order by priority,id limit 1000;
    ```
  - 执行计划
  ```
  +----+-------------+-------+------------+------+-----------------------------+-----------------------------+---------+-------------------+-------+----------+-------------+
  | id | select_type | table | partitions | type | possible_keys               | key                         | key_len | ref               | rows  | filtered | Extra       |
  +----+-------------+-------+------------+------+-----------------------------+-----------------------------+---------+-------------------+-------+----------+-------------+
  |  1 | SIMPLE      | mark  | NULL       | ref  | idx_status_dimen12_priority | idx_status_dimen12_priority | 3       | const,const,const | 23334 |    33.33 | Using where |
  +----+-------------+-------+------------+------+-----------------------------+-----------------------------+---------+-------------------+-------+----------+-------------+
  ```
- 方案二:优先走过滤索引(idx_status_dimen12_ctime) 
    - SQL 耗时:0.10 sec
    ```
    select * from mark force index(idx_status_dimen12_ctime) where status=1 and dimen1 = 90 and dimen2 = 1 and create_time <= unix_timestamp(now())*1000 - 120000 order by priority,id limit 1000;
    ```
  
  - 执行计划
  ```
  +----+-------------+-------+------------+-------+--------------------------+--------------------------+---------+------+-------+----------+-----------------------------+
  | id | select_type | table | partitions | type  | possible_keys            | key                      | key_len | ref  | rows  | filtered | Extra                       |
  +----+-------------+-------+------------+-------+--------------------------+--------------------------+---------+------+-------+----------+-----------------------------+
  |  1 | SIMPLE      | mark  | NULL       | range | idx_status_dimen12_ctime | idx_status_dimen12_ctime | 11      | NULL | 23334 |   100.00 | Using where; Using filesort |
  +----+-------------+-------+------------+-------+--------------------------+--------------------------+---------+------+-------+----------+-----------------------------+  
  ```
  
  - trace:Using filesort
  ```
   "filesort_summary": {
        "rows": 12168,
        "examined_rows": 12168,
        "number_of_tmp_files": 6,
        "sort_buffer_size": 261808,
        "sort_mode": "<sort_key, packed_additional_fields>"
    }  
  ```
- 方案三:[Index Condition Pushdown](https://dev.mysql.com/doc/refman/8.0/en/index-condition-pushdown-optimization.html)
    - 准备工作
    ```
    SELECT @@optimizer_switch\G
    SET optimizer_switch = 'index_condition_pushdown=off';
    SET optimizer_switch = 'index_condition_pushdown=on';
    alter table mark add index idx_status_dimen12_priority_ctime(`status`,`dimen1`,`dimen2`,`priority`,`create_time`);
    ```
  
    - SQL 耗时:0.02 sec
    ```
    select * from mark force index(idx_status_dimen12_priority_ctime) where status=1 and dimen1 = 90 and dimen2 = 1 and create_time <= unix_timestamp(now())*1000 - 120000 order by priority limit 1000;    
    ```
  
    - 执行计划
    ```
    +----+-------------+-------+------------+------+-----------------------------------+-----------------------------------+---------+-------------------+-------+----------+-----------------------+
    | id | select_type | table | partitions | type | possible_keys                     | key                               | key_len | ref               | rows  | filtered | Extra                 |
    +----+-------------+-------+------------+------+-----------------------------------+-----------------------------------+---------+-------------------+-------+----------+-----------------------+
    |  1 | SIMPLE      | mark  | NULL       | ref  | idx_status_dimen12_priority_ctime | idx_status_dimen12_priority_ctime | 3       | const,const,const | 22400 |    33.33 | Using index condition |
    +----+-------------+-------+------------+------+-----------------------------------+-----------------------------------+---------+-------------------+-------+----------+-----------------------+
    ```  
  
    - ICP原理
    
        - 非ICP查询过程:获取索引(idx_status_dimen12_priority) -> 根据索引获取记录 -> 根据where(create_time<=...)过滤记录
        - ICP查询过程:获取索引(idx_status_dimen12_priority_ctime) -> 根据where(create_time<=...)过滤索引 -> 根据索引获取记录 -> 其他where条件过滤记录
  
- 实验
    - 数据准备
        - 把dimen1收敛到0~9
        ```
        update mark set dimen1 = mod(dimen1,10) where dimen1 >= 10;
        ```
        
        - 【status=1 and dimen1 = 1】在create_time上的数据分布
        ```
        +---------------------------------------------------------+----------+
        | date_format(from_unixtime(create_time/1000),'%Y-%m-%d') | count(1) |
        +---------------------------------------------------------+----------+
        | 2019-10-21                                              |    17611 |
        | 2019-10-22                                              |    40766 |
        | 2019-10-23                                              |    14170 |
        | 2019-10-31                                              |     7965 |
        | 2019-11-01                                              |   273420 |
        | 2019-11-02                                              |    52828 |
        +---------------------------------------------------------+----------+  
        ```
      
    - 实验1:idx_status_dimen12_priority&create_time <= unix_timestamp('2019-10-22')*1000
        - SQL 耗时:1.04 sec
        ```
        select * from mark force index(idx_status_dimen12_priority) where status=1 and dimen1 = 1 and dimen2 = 1 and create_time <= unix_timestamp('2019-10-22')*1000 order by priority,id limit 1000;
        ```
      
        - 执行计划
        ```
        +----+-------------+-------+------------+------+-----------------------------+-----------------------------+---------+-------------------+--------+----------+-------------+
        | id | select_type | table | partitions | type | possible_keys               | key                         | key_len | ref               | rows   | filtered | Extra       |
        +----+-------------+-------+------------+------+-----------------------------+-----------------------------+---------+-------------------+--------+----------+-------------+
        |  1 | SIMPLE      | mark  | NULL       | ref  | idx_status_dimen12_priority | idx_status_dimen12_priority | 3       | const,const,const | 217984 |    33.33 | Using where |
        +----+-------------+-------+------------+------+-----------------------------+-----------------------------+---------+-------------------+--------+----------+-------------+
        ```
      
    - 实验2:idx_status_dimen12_priority&create_time <= unix_timestamp('2019-11-02')*1000
        - SQL 耗时:0.02 sec
        ```
        select * from mark force index(idx_status_dimen12_priority) where status=1 and dimen1 = 1 and dimen2 = 1 and create_time <= unix_timestamp('2019-11-02')*1000 order by priority,id limit 1000;
        ```
      
        - 执行计划
        ```
        +----+-------------+-------+------------+------+-----------------------------+-----------------------------+---------+-------------------+--------+----------+-------------+
        | id | select_type | table | partitions | type | possible_keys               | key                         | key_len | ref               | rows   | filtered | Extra       |
        +----+-------------+-------+------------+------+-----------------------------+-----------------------------+---------+-------------------+--------+----------+-------------+
        |  1 | SIMPLE      | mark  | NULL       | ref  | idx_status_dimen12_priority | idx_status_dimen12_priority | 3       | const,const,const | 217984 |    33.33 | Using where |
        +----+-------------+-------+------------+------+-----------------------------+-----------------------------+---------+-------------------+--------+----------+-------------+
        ```
      
    - 实验3:idx_status_dimen12_ctime&create_time <= unix_timestamp('2019-10-22')*1000
        - SQL 耗时:0.02 sec
        ```
        select * from mark force index(idx_status_dimen12_ctime) where status=1 and dimen1 = 1 and dimen2 = 1 and create_time <= unix_timestamp('2019-10-22')*1000 order by priority,id limit 1000;
        ```
              
        - 执行计划
        ```
        +----+-------------+-------+------------+-------+--------------------------+--------------------------+---------+------+------+----------+-----------------------------+
        | id | select_type | table | partitions | type  | possible_keys            | key                      | key_len | ref  | rows | filtered | Extra                       |
        +----+-------------+-------+------------+-------+--------------------------+--------------------------+---------+------+------+----------+-----------------------------+
        |  1 | SIMPLE      | mark  | NULL       | range | idx_status_dimen12_ctime | idx_status_dimen12_ctime | 11      | NULL | 1428 |   100.00 | Using where; Using filesort |
        +----+-------------+-------+------------+-------+--------------------------+--------------------------+---------+------+------+----------+-----------------------------+
        ``` 
             
    - 实验4
        - SQL 耗时:0.88 sec
        ```
        select * from mark force index(idx_status_dimen12_ctime) where status=1 and dimen1 = 1 and dimen2 = 1 and create_time <= unix_timestamp('2019-11-02')*1000 order by priority,id limit 1000;
        ```
              
        - 执行计划 
        ```
        +----+-------------+-------+------------+-------+--------------------------+--------------------------+---------+------+--------+----------+-----------------------------+
        | id | select_type | table | partitions | type  | possible_keys            | key                      | key_len | ref  | rows   | filtered | Extra                       |
        +----+-------------+-------+------------+-------+--------------------------+--------------------------+---------+------+--------+----------+-----------------------------+
        |  1 | SIMPLE      | mark  | NULL       | range | idx_status_dimen12_ctime | idx_status_dimen12_ctime | 11      | NULL | 206928 |   100.00 | Using where; Using filesort |
        +----+-------------+-------+------------+-------+--------------------------+--------------------------+---------+------+--------+----------+-----------------------------+
        ```
      
        - trace:Using filesort
        ```
            "filesort_summary": {
              "rows": 99713,
              "examined_rows": 99713,
              "number_of_tmp_files": 42,
              "sort_buffer_size": 261808,
              "sort_mode": "<sort_key, packed_additional_fields>"
            }
        ```
                     
    - 实验5:idx_status_dimen12_priority_ctime&create_time <= unix_timestamp('2019-10-22')*1000
        - SQL 耗时:0.06 sec
        ```
        select * from mark force index(idx_status_dimen12_priority_ctime) where status=1 and dimen1 = 1 and dimen2 = 1 and create_time <= unix_timestamp('2019-10-22')*1000 order by priority,create_time,id limit 1000;
        ```
              
        - 执行计划
        ```
        +----+-------------+-------+------------+------+-----------------------------------+-----------------------------------+---------+-------------------+--------+----------+-----------------------+
        | id | select_type | table | partitions | type | possible_keys                     | key                               | key_len | ref               | rows   | filtered | Extra                 |
        +----+-------------+-------+------------+------+-----------------------------------+-----------------------------------+---------+-------------------+--------+----------+-----------------------+
        |  1 | SIMPLE      | mark  | NULL       | ref  | idx_status_dimen12_priority_ctime | idx_status_dimen12_priority_ctime | 3       | const,const,const | 222336 |    33.33 | Using index condition |
        +----+-------------+-------+------------+------+-----------------------------------+-----------------------------------+---------+-------------------+--------+----------+-----------------------+
        ```
              
    - 实验6:idx_status_dimen12_priority_ctime&create_time <= unix_timestamp('2019-11-02')*1000
        - SQL 耗时:0.01 sec
        ```
        select * from mark force index(idx_status_dimen12_priority_ctime) where status=1 and dimen1 = 1 and dimen2 = 1 and create_time <= unix_timestamp('2019-11-02')*1000 order by priority,create_time,id limit 1000;
        ```
              
        - 执行计划
        ```
        +----+-------------+-------+------------+------+-----------------------------------+-----------------------------------+---------+-------------------+--------+----------+-----------------------+
        | id | select_type | table | partitions | type | possible_keys                     | key                               | key_len | ref               | rows   | filtered | Extra                 |
        +----+-------------+-------+------------+------+-----------------------------------+-----------------------------------+---------+-------------------+--------+----------+-----------------------+
        |  1 | SIMPLE      | mark  | NULL       | ref  | idx_status_dimen12_priority_ctime | idx_status_dimen12_priority_ctime | 3       | const,const,const | 222336 |    33.33 | Using index condition |
        +----+-------------+-------+------------+------+-----------------------------------+-----------------------------------+---------+-------------------+--------+----------+-----------------------+
        ```        
        
- 总结&分析
    -  排序字段、过滤字段不一致时，优先过滤还是排序?
        - 过滤效果强时，过滤后剩余记录少，filesort能在内存中完成，可以选择过滤索引；过滤效果弱时，则选择排序索引效果更好，比如说业务中使用到的场景(create_time<unix_timestamp(now())*1000 - 120000)。
        - 排序字段、过滤字段在顺序上是否相关，eg:如果有当r1.priority>r2.priority时，则r1.create_time>r2.create_time,则可以选择排序索引。
        
    - ICP，其标志是执行计划中包含【Using index condition】
        - 过滤效果强时，ICP略差于走过滤索引，远好于走排序索引。
        - 过滤效果弱时，ICP与走排序索引基本一致，远好于走过滤索引。
        
    - 从方案二、实验4的trace中可以看出filesort使用了磁盘文件排序("number_of_tmp_files": 6/"number_of_tmp_files": 42)。
          
### 7.多dimen数据查询应该如何设计？
- 查询语句
    ```
    select * from mark where status=1 and dimen1 in(0,1) and dimen2 in(0,1) and create_time <= unix_timestamp(now())*1000 - 120000 order by priority limit 1000;
    ```
- 方案一:优先走排序索引(idx_status_dimen12_priority)
    - 查询1 耗时:4.63 sec
        - SQL:create_time <= unix_timestamp('2019-10-22')*1000
            ```
            select * from mark force index(idx_status_dimen12_priority) where status=1 and dimen1 in(0,1) and dimen2 in(0,1) and create_time <= unix_timestamp('2019-10-22')*1000 order by priority limit 1000;
            ```
        - 执行计划
            ```
            +----+-------------+-------+------------+-------+-----------------------------+-----------------------------+---------+------+--------+----------+-----------------------------+
            | id | select_type | table | partitions | type  | possible_keys               | key                         | key_len | ref  | rows   | filtered | Extra                       |
            +----+-------------+-------+------------+-------+-----------------------------+-----------------------------+---------+------+--------+----------+-----------------------------+
            |  1 | SIMPLE      | mark  | NULL       | range | idx_status_dimen12_priority | idx_status_dimen12_priority | 3       | NULL | 997502 |    33.33 | Using where; Using filesort |
            +----+-------------+-------+------------+-------+-----------------------------+-----------------------------+---------+------+--------+----------+-----------------------------+
            ```
        - trace:Using filesort
            ```
            "filesort_summary": {
              "rows": 13512,
              "examined_rows": 497198,
              "number_of_tmp_files": 5,
              "sort_buffer_size": 261712,
              "sort_mode": "<sort_key, packed_additional_fields>"
            }            
            ```
    - 查询2 耗时:4.77 sec
        - SQL:create_time <= unix_timestamp('2019-11-02')*1000
            ```
            select * from mark force index(idx_status_dimen12_priority) where status=1 and dimen1 in(0,1) and dimen2 in(0,1) and create_time <= unix_timestamp('2019-11-02')*1000 order by priority limit 1000;
            ```
        - 执行计划
            ```
            +----+-------------+-------+------------+-------+-----------------------------+-----------------------------+---------+------+--------+----------+-----------------------------+
            | id | select_type | table | partitions | type  | possible_keys               | key                         | key_len | ref  | rows   | filtered | Extra                       |
            +----+-------------+-------+------------+-------+-----------------------------+-----------------------------+---------+------+--------+----------+-----------------------------+
            |  1 | SIMPLE      | mark  | NULL       | range | idx_status_dimen12_priority | idx_status_dimen12_priority | 3       | NULL | 997502 |    33.33 | Using where; Using filesort |
            +----+-------------+-------+------------+-------+-----------------------------+-----------------------------+---------+------+--------+----------+-----------------------------+
            ```
        - trace:Using filesort
            ```
            "filesort_summary": {
              "rows": 427462,
              "examined_rows": 497198,
              "number_of_tmp_files": 164,
              "sort_buffer_size": 261712,
              "sort_mode": "<sort_key, packed_additional_fields>"
            }            
            ```

- 方案二:优先走排序索引(idx_status_dimen12_ctime)
    - 查询1 耗时:0.10 sec
        - SQL:create_time <= unix_timestamp('2019-10-22')*1000
            ```
            select * from mark force index(idx_status_dimen12_ctime) where status=1 and dimen1 in(0,1) and dimen2 in(0,1) and create_time <= unix_timestamp('2019-10-22')*1000 order by priority limit 1000;
            ```
        - 执行计划
            ```
            +----+-------------+-------+------------+-------+--------------------------+--------------------------+---------+------+-------+----------+-----------------------------+
            | id | select_type | table | partitions | type  | possible_keys            | key                      | key_len | ref  | rows  | filtered | Extra                       |
            +----+-------------+-------+------------+-------+--------------------------+--------------------------+---------+------+-------+----------+-----------------------------+
            |  1 | SIMPLE      | mark  | NULL       | range | idx_status_dimen12_ctime | idx_status_dimen12_ctime | 11      | NULL | 22615 |   100.00 | Using where; Using filesort |
            +----+-------------+-------+------------+-------+--------------------------+--------------------------+---------+------+-------+----------+-----------------------------+
            ```
        - trace:Using filesort
            ```
            "filesort_summary": {
              "rows": 13512,
              "examined_rows": 13512,
              "number_of_tmp_files": 5,
              "sort_buffer_size": 261712,
              "sort_mode": "<sort_key, packed_additional_fields>"
            }          
            ```
    - 查询2 耗时:3.83 sec
        - SQL:create_time <= unix_timestamp('2019-11-02')*1000
            ```
            select * from mark force index(idx_status_dimen12_ctime) where status=1 and dimen1 in(0,1) and dimen2 in(0,1) and create_time <= unix_timestamp('2019-11-02')*1000 order by priority limit 1000;
            ```
        - 执行计划
            ```
            +----+-------------+-------+------------+-------+--------------------------+--------------------------+---------+------+---------+----------+-----------------------------+
            | id | select_type | table | partitions | type  | possible_keys            | key                      | key_len | ref  | rows    | filtered | Extra                       |
            +----+-------------+-------+------------+-------+--------------------------+--------------------------+---------+------+---------+----------+-----------------------------+
            |  1 | SIMPLE      | mark  | NULL       | range | idx_status_dimen12_ctime | idx_status_dimen12_ctime | 11      | NULL | 1021740 |   100.00 | Using where; Using filesort |
            +----+-------------+-------+------------+-------+--------------------------+--------------------------+---------+------+---------+----------+-----------------------------+
            ```
        - trace:Using filesort
            ```
            "filesort_summary": {
              "rows": 427462,
              "examined_rows": 427462,
              "number_of_tmp_files": 164,
              "sort_buffer_size": 261712,
              "sort_mode": "<sort_key, packed_additional_fields>"
            }
            ```
- 方案三:拆解SQL+应用代码排序
    - 查询一:create_time <= unix_timestamp('2019-10-22')*1000
        - SQL1 耗时:0.03 sec
            ```
            select * from mark where status=1 and dimen1 = 0 and dimen2 = 0 and create_time <= unix_timestamp('2019-10-22')*1000 order by priority,create_time,id limit 1000;
            ```
        - SQL2 耗时:0.01 sec
            ```
            select * from mark where status=1 and dimen1 = 0 and dimen2 = 1 and create_time <= unix_timestamp('2019-10-22')*1000 order by priority,create_time,id limit 1000;
            ```    
        - SQL3 耗时:0.04 sec
            ```
            select * from mark where status=1 and dimen1 = 1 and dimen2 = 0 and create_time <= unix_timestamp('2019-10-22')*1000 order by priority,create_time,id limit 1000;
            ```    
        - SQL4 耗时:0.01 sec
            ```
            select * from mark where status=1 and dimen1 = 1 and dimen2 = 2 and create_time <= unix_timestamp('2019-10-22')*1000 order by priority,create_time,id limit 1000;
            ```
    - 查询二:create_time <= unix_timestamp('2019-11-02')*1000
       - SQL1 耗时:0.01 sec
            ```
            select * from mark force index(idx_status_dimen12_priority_ctime) where status=1 and dimen1 = 0 and dimen2 = 0 and create_time <= unix_timestamp('2019-11-02')*1000 order by priority,create_time,id limit 1000;
            ```
        - SQL2 耗时:0.01 sec
            ```
            select * from mark force index(idx_status_dimen12_priority_ctime) where status=1 and dimen1 = 0 and dimen2 = 1 and create_time <= unix_timestamp('2019-11-02')*1000 order by priority,create_time,id limit 1000;
            ```    
        - SQL3 耗时:0.01 sec
            ```
          
            select * from mark force index(idx_status_dimen12_priority_ctime) where status=1 and dimen1 = 1 and dimen2 = 0 and create_time <= unix_timestamp('2019-11-02')*1000 order by priority,create_time,id limit 1000;
            ```
              
        - SQL4 耗时:0.01 sec
            ```
            select * from mark force index(idx_status_dimen12_priority_ctime) where status=1 and dimen1 = 1 and dimen2 = 2 and create_time <= unix_timestamp('2019-11-02')*1000 order by priority,create_time,id limit 1000;  
            ```  
- 总结&分析
    - 当排序字段的前缀字段有多个值(in,>=,<=...)时，无法使用排序索引，虽然通过多路归并排序可以加快性能，但是MySQL并未直接这么多做。
    - 当经过create_time过滤后剩余数据量比较小时，适合走过滤索引(idx_status_dimen12_ctime),filesort可以在内存完成。
    - 当经过create_time过滤后剩余数据量比较大时，没有合适的索引可以走，可以考虑将SQL拆解为多个SQL分别查询再在应用服务上合并。     
### 8.order by + limit 翻页
- 场景一
    - 添加索引
        ```
        alter table mark add index idx_priority(priority);
        ```
    - SQL1 耗时:3.92 sec
        ```
        select * from mark where dimen2 = 1 order by priority limit 100000,10;
        ```
    - SQL2 耗时:0.00 sec
        ```
        select * from mark where dimen2 = 1 and ((priority = 47139845304 and id > 9791227) or priority > 47139845304) order by priority limit 100;
        ```      
- 场景二
    - 添加索引
        ```
        alter table mark add index idx_dimen2_priority(dimen2,priority);
        ```
    - SQL1 耗时:1.48 sec
        ```
        select * from mark where dimen2 = 1 order by priority limit 100000,100;
        ```     
    - SQL2 耗时:0.00 sec
        ```
        select * from mark where dimen2 = 1 and ((priority = 47139845304 and id > 9791227) or priority > 47139845304) order by priority limit 100;
        ```
- 总结&分析
    - 使用limit :offset,:limit分页查询时，MySQL的实现是:Server层去engine层取一批数据->Server层过滤并判断需不需要再取一批数据，所以至少需要取offset+limit行数据，offset越大，扫描的行或索引越多。
    - 分页查询的一种可替换方案是，利用上一次查询记录消除limit中offset，如上例中的【((priority = 47139845304 and id > 9791227) or priority > 47139845304)】。
### 9.数据备份的一次故障分析
- 需求
    
   把mark中30天之前且【status=2】的数据备份kshard中。
    
- 方案
    ```
    minId = 0;
    deadline = now() - 30d;
    do{
        select * from mark where status = 2 and create_time <= :deadline and id > :minId order by id limit 1000;
        insert into kshard;
        delete from mark where id in(:ids);
        minId = :lastId;
        if(count < 1000) break;
    }while(true);
    ```
  
- 故障分析

    有一个测试业务，其status的值是随机的，【status=0,1,2】是均匀分布的，而且数据量很大，第一次备份时会把【status=2】的数据全删除，剩下了一批【status=0,1】的数据，数据量还很大，第二次备份时MySQL需要对这部分数据做一次全部扫描，所以第一条SQL就会把数据库拖垮。PS:解释一下需要【id > :minId&order by id】的原因，备份查备库，删除主库，如果没有这个条件，主备延迟容易引起重复备份。
     
### 10.others
- 函数优化

    select max(from_unixtime(create_time/1000)) -> select from_unixtime(max(create_time)/1000)

- [Multi-Range Read Optimization](https://dev.mysql.com/doc/refman/8.0/en/mrr-optimization.html)

- NamedParameterJdbcTemplate.batchUpdate的返回值是影响的行数吗？
    - 源码片段
        ```
        #com.mysql.jdbc.PreparedStatement.executeBatchedInserts
       if (numBatchedArgs > 1) {
            long updCount = updateCountRunningTotal > 0 ? java.sql.Statement.SUCCESS_NO_INFO : 0;
            for (int j = 0; j < numBatchedArgs; j++) {
                updateCounts[j] = updCount;
            }
        } else {
            updateCounts[0] = updateCountRunningTotal;
        }        
        ```
      
    - 注意
    
        只有单行更新时，batchUpdate才返回影响的行数，多行时返回的是java.sql.Statement.SUCCESS_NO_INFO(-2)。    

- NamedParameterJdbcTemplate只会把【:property】解析为动态变量【?】吗？
    - 源码片段
        ```
        #org.springframework.jdbc.core.namedparam.NamedParameterUtils.parseSqlStatement
        if (c == ':' || c == '&'){
            ......
        }
        ```
    - 注意
    
        MySQL支持逻辑与(&)操作,使用时一定要在&后面加空白符，不然会被误认为动态变量。    
### 11.多维度分页查询的一种优化方案

待续



