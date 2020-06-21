# MySQL的N次踩坑经验总结（二）
## 零. 准备工作

- 系统环境

    ```
    mysql> select version();
    +-----------+
    | version() |
    +-----------+
    | 8.0.19    |
    +-----------+
  
    mysql> show variables like '%iso%';
    +-----------------------+-----------------+
    | Variable_name         | Value           |
    +-----------------------+-----------------+
    | transaction_isolation | REPEATABLE-READ |
    +-----------------------+-----------------+
    ```
  
- 表结构

    ```
    CREATE TABLE `stu` (
      `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
      `no` bigint unsigned NOT NULL COMMENT 'no',
      `name` varchar(30) NOT NULL DEFAULT '' COMMENT 'name',
      `sex` tinyint DEFAULT NULL COMMENT 'sex',
      `age` tinyint DEFAULT NULL COMMENT 'age',
      `remark` varchar(100) NOT NULL DEFAULT '' COMMENT 'remark',
      PRIMARY KEY (`id`),
      UNIQUE KEY `uidx_no` (`no`),
      KEY `idx_name` (`name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
    ```
  
- 数据准备
    ```
    insert into 
  stu(id,no,name,age,sex,remark) 
  values
  (1,5,'B',2,1,'ZZZ'),
  (2,10,'D',39,0,'YYY'),
  (3,15,'F',26,1,'XXX'),
  (4,20,'F',79,0,'WWW'),
  (5,25,'I',68,1,'VVV'),
  (6,30,'K',44,0,'UUU');
    ```
  
## 一.锁简介
### 1.[表锁](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html#innodb-shared-exclusive-locks)
##### &emsp; 1) 类型及简介
- [X/S](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html#innodb-shared-exclusive-locks) :在Server层锁定整个表。
    ```
    LOCK TABLES ... READ/WRITE
    ```
- [IX/IS](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html#innodb-intention-locks) :意向锁,DML语句产生。
    ```
    insert...
    update...
    delete...
    select... for share/update
    ```
- [AI](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html#innodb-auto-inc-locks) :自增锁,INSERT生成自增列值时使用,非事务锁,有三种模式,由参数[innodb_autoinc_lock_mode](https://dev.mysql.com/doc/refman/5.7/en/innodb-auto-increment-handling.html) 控制。 
    ```
    非事务锁:不等事务commit之后释放，以每条SQL语句为单位，释放时机有三种情况:
        1) SQL执行之前
        2) SQL执行之后
        3) 对于Bulk insert 每行获取/释放一次
    具体由参数innodb_autoinc_lock_mode控制。
    ```

##### &emsp; 2) 兼容-冲突规则
- 规则矩阵

     Second/First|AI|X|IX|S|IS
    ---|---|---|---|---|---
    AI|Conflict|Conflict|Compatible|Conflict|Compatible
    X|Conflict|Conflict|Conflict|Conflict|Conflict
    IX|Compatible|Conflict|Compatible|Conflict|Compatible
    S|Conflict|Conflict|Conflict|Compatible|Compatible
    IS|Compatible|Conflict|Compatible|Compatible|Compatible

### 2.行锁
##### &emsp; 1) 类型及简介
- Record Locks
    > A record lock is a lock on an index record.
    ```
    update stu set remark = 'RL' where id = 1;
    ```
- Gap Locks
    > A gap lock is a lock on a gap between index records, or a lock on the gap before the first or after the last index record.
    ```
    update stu set remark = 'RL' where id = 2;
    ```
- Next-Key Locks
    > A next-key lock is a combination of a record lock on the index record and a gap lock on the gap before the index record.
    ```
    update stu set remark = 'RL' where id <= 2;
    ```
- Insert Intention Locks
    > An insert intention lock is a type of gap lock set by INSERT operations prior to row insertion.
    ```
    insert into stu(no,name,remark) values(2,'IIL','IIL');
    ```
##### &emsp; 2) 兼容-冲突规则
- 规则矩阵

    Second/First|S-Record|X-Record|S-Next Key|X-Next Key|Gap|Insert Intention
    ---|---|---|---|---|---|---
    S-Record|Compatible|Conflict|Compatible|Conflict|Compatible|Compatible
    X-Record|Conflict|Conflict|Conflict|Conflict|Compatible|Compatible
    S-Next Key|Compatible|Conflict|Compatible|Conflict|Compatible|Compatible
    X-Next Key|Conflict|Conflict|Conflict|Conflict|Compatible|Compatible
    Gap|Compatible|Compatible|Compatible|Compatible|Compatible|Compatible
    Insert Intention|Compatible|Compatible|Conflict|Conflict|Conflict|Compatible
### 3.explict vs implict Lock
##### &emsp; 1) explict Lock
- 理解

    立即获取锁，比如:执行【update stu set remark = 'RL' where id = 1】,在【performance_schema.data_locks】必然会存在【IX】表锁和id=1的【X,REC_NOT_GAP】记录锁。
##### &emsp; 2) implict Lock

- 理解

    延迟获取锁，比如:执行【insert into stu(no,name,remark) values(2,'IIL','IIL')】, 在【performance_schema.data_locks】中只有【IX】表锁，但是其实为了ACID还应该加id=2的记录锁、no在(1,5]上的间隙锁。那么延迟到什么时候获取该加的锁呢？答:当发现锁冲突的时候。

- 怎么发现锁冲突？
    - 聚集索引记录
        
        每个聚集索引记录都有【DB_TRX_ID】和【DB_ROLL_PTR】，分别记录最近操作事务ID和前一个版本的undo日志指针,检测【DB_TRX_ID】是否活跃，来判断是否需要加锁。
    
    - 辅助索引记录 
        
        辅助索引页有【PAGE_MAX_TRX_ID】，页内每条记录变动都会更新这个值，检测时先判断【PAGE_MAX_TRX_ID】是否比最小活跃事务还小，如果【PAGE_MAX_TRX_ID】小直接判定为不冲突，否则需要回溯到聚集索引记录，根据【DB_TRX_ID】/【DB_ROLL_PTR】及记录值，再综合锁的兼容规则判断是否冲突。
     
### 4.Lock vs Latch
##### &emsp; 1) Lock
- 说明

    1、2、3介绍的都是Lock，一般为事务锁(AI除外)，针对的是表及表中记录。
##### &emsp; 1) Latch
- 说明

    是线程锁，比Lock量级轻，保护内存数据结构，一般都能很快释放。
## 三.SQL行锁分析
### 1.where:update/delete/select ... for share/select ... for update
##### &emsp; 1) 主键
- CASE1:等值&存在/不存在
    - SQL
    ```
    select * from stu where id in(0,3,7) for update;
    ```
    - 加锁情况
    ```
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+------------------------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA              |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+------------------------+
    |                  2886 |        61 |      244 | NULL       | TABLE     | IX            | GRANTED     | NULL                   |
    |                  2886 |        61 |      244 | PRIMARY    | RECORD    | X,GAP         | GRANTED     | 1                      |
    |                  2886 |        61 |      244 | PRIMARY    | RECORD    | X,REC_NOT_GAP | GRANTED     | 3                      |
    |                  2886 |        61 |      244 | PRIMARY    | RECORD    | X             | GRANTED     | supremum pseudo-record |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+------------------------+
   说明：supremum是伪列，加在其上的next-key锁等价于gap锁。
    ```

- CASE2:范围【 < 】
    - SQL
    ```
    select * from stu where id < 2 for update;
    ```
    - 加锁情况
    ```
    +-----------------------+-----------+----------+------------+-----------+-----------+-------------+-----------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE | LOCK_STATUS | LOCK_DATA |
    +-----------------------+-----------+----------+------------+-----------+-----------+-------------+-----------+
    |                  2976 |        61 |      323 | NULL       | TABLE     | IX        | GRANTED     | NULL      |
    |                  2976 |        61 |      323 | PRIMARY    | RECORD    | X         | GRANTED     | 1         |
    |                  2976 |        61 |      323 | PRIMARY    | RECORD    | X,GAP     | GRANTED     | 2         |
    +-----------------------+-----------+----------+------------+-----------+-----------+-------------+-----------+
    ```
- CASE3:范围【 <= 】
    - SQL
    ```
    select * from stu where id <= 2 for update;
    ```
    - 加锁情况
    ```
    +-----------------------+-----------+----------+------------+-----------+-----------+-------------+-----------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE | LOCK_STATUS | LOCK_DATA |
    +-----------------------+-----------+----------+------------+-----------+-----------+-------------+-----------+
    |                  2977 |        61 |      326 | NULL       | TABLE     | IX        | GRANTED     | NULL      |
    |                  2977 |        61 |      326 | PRIMARY    | RECORD    | X         | GRANTED     | 1         |
    |                  2977 |        61 |      326 | PRIMARY    | RECORD    | X         | GRANTED     | 2         |
    +-----------------------+-----------+----------+------------+-----------+-----------+-------------+-----------+
    ``` 
- CASE4:范围【 > 】
    - SQL
    ```
    select * from stu where id > 5 for update;
    ```
    - 加锁情况
    ```
    +-----------------------+-----------+----------+------------+-----------+-----------+-------------+------------------------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE | LOCK_STATUS | LOCK_DATA              |
    +-----------------------+-----------+----------+------------+-----------+-----------+-------------+------------------------+
    |                  2978 |        61 |      329 | NULL       | TABLE     | IX        | GRANTED     | NULL                   |
    |                  2978 |        61 |      329 | PRIMARY    | RECORD    | X         | GRANTED     | supremum pseudo-record |
    |                  2978 |        61 |      329 | PRIMARY    | RECORD    | X         | GRANTED     | 6                      |
    +-----------------------+-----------+----------+------------+-----------+-----------+-------------+------------------------+
    ```
- CASE5:范围【 >= 】
    - SQL
    ```
    select * from stu where id >= 5 for update;
    ```
    - 加锁情况
    ```
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+------------------------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA              |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+------------------------+
    |                  2979 |        61 |      332 | NULL       | TABLE     | IX            | GRANTED     | NULL                   |
    |                  2979 |        61 |      332 | PRIMARY    | RECORD    | X             | GRANTED     | supremum pseudo-record |
    |                  2979 |        61 |      332 | PRIMARY    | RECORD    | X,REC_NOT_GAP | GRANTED     | 5                      |
    |                  2979 |        61 |      332 | PRIMARY    | RECORD    | X             | GRANTED     | 6                      |
    ```     
##### &emsp; 1) 唯一键
- CASE1:等值&存在/不存在
    - SQL
    ```
    select * from stu where no in(0,10,35) for update;
    ```
    - 加锁情况
    ```
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+------------------------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA              |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+------------------------+
    |                  2889 |        61 |      253 | NULL       | TABLE     | IX            | GRANTED     | NULL                   |
    |                  2889 |        61 |      253 | uidx_no    | RECORD    | X,GAP         | GRANTED     | 5, 1                   |
    |                  2889 |        61 |      253 | uidx_no    | RECORD    | X,REC_NOT_GAP | GRANTED     | 10, 2                  |
    |                  2889 |        61 |      253 | PRIMARY    | RECORD    | X,REC_NOT_GAP | GRANTED     | 2                      |
    |                  2889 |        61 |      253 | uidx_no    | RECORD    | X             | GRANTED     | supremum pseudo-record |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+------------------------+
    ```
  
- CASE2:范围【 < 】

    - SQL
    ```
    select * from stu where no < 10 for update;
    ```  
    - 加锁情况
    ```
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    |                  2980 |        61 |      335 | NULL       | TABLE     | IX            | GRANTED     | NULL      |
    |                  2980 |        61 |      335 | PRIMARY    | RECORD    | X,REC_NOT_GAP | GRANTED     | 1         |
    |                  2980 |        61 |      335 | uidx_no    | RECORD    | X             | GRANTED     | 5, 1      |
    |                  2980 |        61 |      335 | uidx_no    | RECORD    | X             | GRANTED     | 10, 2     |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    ```
- CASE3:范围【 <= 】

    - SQL
    ```
    select * from stu where no <= 10 for update;
    ```  
    - 加锁情况
    ```
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    |                  2981 |        61 |      338 | NULL       | TABLE     | IX            | GRANTED     | NULL      |
    |                  2981 |        61 |      338 | PRIMARY    | RECORD    | X,REC_NOT_GAP | GRANTED     | 1         |
    |                  2981 |        61 |      338 | PRIMARY    | RECORD    | X,REC_NOT_GAP | GRANTED     | 2         |
    |                  2981 |        61 |      338 | uidx_no    | RECORD    | X             | GRANTED     | 5, 1      |
    |                  2981 |        61 |      338 | uidx_no    | RECORD    | X             | GRANTED     | 10, 2     |
    |                  2981 |        61 |      338 | uidx_no    | RECORD    | X             | GRANTED     | 15, 3     |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    ```
- CASE4:范围【 > 】

    - SQL
    ```
    select * from stu where no > 25 for update;
    ```  
    - 加锁情况
    ```
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+------------------------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA              |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+------------------------+
    |                  2982 |        61 |      342 | NULL       | TABLE     | IX            | GRANTED     | NULL                   |
    |                  2982 |        61 |      342 | PRIMARY    | RECORD    | X,REC_NOT_GAP | GRANTED     | 6                      |
    |                  2982 |        61 |      342 | uidx_no    | RECORD    | X             | GRANTED     | supremum pseudo-record |
    |                  2982 |        61 |      342 | uidx_no    | RECORD    | X             | GRANTED     | 30, 6                  |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+------------------------+
    ```
- CASE5:范围【 >= 】

    - SQL
    ```
    select * from stu where no >= 25 for update;
    ```  
    - 加锁情况
    ```
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+------------------------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA              |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+------------------------+
    |                  2983 |        61 |      345 | NULL       | TABLE     | IX            | GRANTED     | NULL                   |
    |                  2983 |        61 |      345 | PRIMARY    | RECORD    | X,REC_NOT_GAP | GRANTED     | 5                      |
    |                  2983 |        61 |      345 | PRIMARY    | RECORD    | X,REC_NOT_GAP | GRANTED     | 6                      |
    |                  2983 |        61 |      345 | uidx_no    | RECORD    | X             | GRANTED     | supremum pseudo-record |
    |                  2983 |        61 |      345 | uidx_no    | RECORD    | X             | GRANTED     | 25, 5                  |
    |                  2983 |        61 |      345 | uidx_no    | RECORD    | X             | GRANTED     | 30, 6                  |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+------------------------+
    ```       
##### &emsp; 3) 非唯一键
- CASE1:等值&存在/不存在
    - SQL
    ```
    select * from stu where name in('A','D','Z') for update;
    ```
    - 加锁情况
    ```
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+------------------------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | INDEX_NAME | OBJECT_INSTANCE_BEGIN | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA              |
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+------------------------+
    |                  4118 |        53 | NULL       |       140683790861672 | TABLE     | IX            | GRANTED     | NULL                   |
    |                  4118 |        53 | idx_name   |       140683633669656 | RECORD    | X,GAP         | GRANTED     | 'B', 1                 |
    |                  4118 |        53 | idx_name   |       140683633669656 | RECORD    | X,GAP         | GRANTED     | 'F', 3                 |
    |                  4118 |        53 | idx_name   |       140683633670000 | RECORD    | X             | GRANTED     | supremum pseudo-record |
    |                  4118 |        53 | idx_name   |       140683633670000 | RECORD    | X             | GRANTED     | 'D', 2                 |
    |                  4118 |        53 | PRIMARY    |       140683633670344 | RECORD    | X,REC_NOT_GAP | GRANTED     | 2                      |
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+------------------------+
    ```
- CASE2:范围

    - SQL
    ```
    select * from stu where name < 'F' for update;
    ```
    - 加锁情况
    ```
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+-----------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | INDEX_NAME | OBJECT_INSTANCE_BEGIN | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA |
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+-----------+
    |                  4119 |        53 | NULL       |       140683790861672 | TABLE     | IX            | GRANTED     | NULL      |
    |                  4119 |        53 | idx_name   |       140683633669656 | RECORD    | X             | GRANTED     | 'B', 1    |
    |                  4119 |        53 | idx_name   |       140683633669656 | RECORD    | X             | GRANTED     | 'D', 2    |
    |                  4119 |        53 | idx_name   |       140683633669656 | RECORD    | X             | GRANTED     | 'F', 3    |
    |                  4119 |        53 | PRIMARY    |       140683633670000 | RECORD    | X,REC_NOT_GAP | GRANTED     | 1         |
    |                  4119 |        53 | PRIMARY    |       140683633670000 | RECORD    | X,REC_NOT_GAP | GRANTED     | 2         |
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+-----------+
    ```
- CASE3:范围【 <= 】

    - SQL
    ```
    select * from stu where name <= 'F' for update;
    ```
    - 加锁情况
    ```
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+-----------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | INDEX_NAME | OBJECT_INSTANCE_BEGIN | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA |
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+-----------+
    |                  4120 |        53 | NULL       |       140683790861672 | TABLE     | IX            | GRANTED     | NULL      |
    |                  4120 |        53 | idx_name   |       140683633669656 | RECORD    | X             | GRANTED     | 'B', 1    |
    |                  4120 |        53 | idx_name   |       140683633669656 | RECORD    | X             | GRANTED     | 'D', 2    |
    |                  4120 |        53 | idx_name   |       140683633669656 | RECORD    | X             | GRANTED     | 'F', 3    |
    |                  4120 |        53 | idx_name   |       140683633669656 | RECORD    | X             | GRANTED     | 'F', 4    |
    |                  4120 |        53 | idx_name   |       140683633669656 | RECORD    | X             | GRANTED     | 'I', 5    |
    |                  4120 |        53 | PRIMARY    |       140683633670000 | RECORD    | X,REC_NOT_GAP | GRANTED     | 1         |
    |                  4120 |        53 | PRIMARY    |       140683633670000 | RECORD    | X,REC_NOT_GAP | GRANTED     | 2         |
    |                  4120 |        53 | PRIMARY    |       140683633670000 | RECORD    | X,REC_NOT_GAP | GRANTED     | 3         |
    |                  4120 |        53 | PRIMARY    |       140683633670000 | RECORD    | X,REC_NOT_GAP | GRANTED     | 4         |
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+-----------+
    ```
- CASE4:范围【 > 】

    - SQL
    ```
    select * from stu where name > 'F' for update;
    ```
    - 加锁情况
    ```
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+------------------------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | INDEX_NAME | OBJECT_INSTANCE_BEGIN | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA              |
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+------------------------+
    |                  4122 |        53 | NULL       |       140683790861672 | TABLE     | IX            | GRANTED     | NULL                   |
    |                  4122 |        53 | idx_name   |       140683633669656 | RECORD    | X             | GRANTED     | supremum pseudo-record |
    |                  4122 |        53 | idx_name   |       140683633669656 | RECORD    | X             | GRANTED     | 'I', 5                 |
    |                  4122 |        53 | idx_name   |       140683633669656 | RECORD    | X             | GRANTED     | 'K', 6                 |
    |                  4122 |        53 | PRIMARY    |       140683633670000 | RECORD    | X,REC_NOT_GAP | GRANTED     | 5                      |
    |                  4122 |        53 | PRIMARY    |       140683633670000 | RECORD    | X,REC_NOT_GAP | GRANTED     | 6                      |
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+------------------------+
    ```
- CASE5:范围【 >= 】

    - SQL
    ```
    select * from stu where name >= 'F' for update;
    ```
    - 加锁情况
    ```
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+------------------------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | INDEX_NAME | OBJECT_INSTANCE_BEGIN | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA              |
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+------------------------+
    |                  4123 |        53 | NULL       |       140683790861672 | TABLE     | IX            | GRANTED     | NULL                   |
    |                  4123 |        53 | idx_name   |       140683633669656 | RECORD    | X             | GRANTED     | supremum pseudo-record |
    |                  4123 |        53 | idx_name   |       140683633669656 | RECORD    | X             | GRANTED     | 'F', 3                 |
    |                  4123 |        53 | idx_name   |       140683633669656 | RECORD    | X             | GRANTED     | 'F', 4                 |
    |                  4123 |        53 | idx_name   |       140683633669656 | RECORD    | X             | GRANTED     | 'I', 5                 |
    |                  4123 |        53 | idx_name   |       140683633669656 | RECORD    | X             | GRANTED     | 'K', 6                 |
    |                  4123 |        53 | PRIMARY    |       140683633670000 | RECORD    | X,REC_NOT_GAP | GRANTED     | 3                      |
    |                  4123 |        53 | PRIMARY    |       140683633670000 | RECORD    | X,REC_NOT_GAP | GRANTED     | 4                      |
    |                  4123 |        53 | PRIMARY    |       140683633670000 | RECORD    | X,REC_NOT_GAP | GRANTED     | 5                      |
    |                  4123 |        53 | PRIMARY    |       140683633670000 | RECORD    | X,REC_NOT_GAP | GRANTED     | 6                      |
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+------------------------+
    ```
      
##### &emsp; 4) 全表扫描
- CASE 【 =/</<=/>/>= 】
    - SQL
    ```
    select * from stu where sex = '0' for update;
    select * from stu where sex < '0' for update;
    select * from stu where sex <= '0' for update;
    select * from stu where sex > '0' for update;
    select * from stu where sex >= '0' for update;
    ```
    - 加锁情况
    ```
    +-----------------------+-----------+----------+------------+-----------+-----------+-------------+------------------------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE | LOCK_STATUS | LOCK_DATA              |
    +-----------------------+-----------+----------+------------+-----------+-----------+-------------+------------------------+
    |                  2990 |        61 |      372 | NULL       | TABLE     | IX        | GRANTED     | NULL                   |
    |                  2990 |        61 |      372 | PRIMARY    | RECORD    | X         | GRANTED     | supremum pseudo-record |
    |                  2990 |        61 |      372 | PRIMARY    | RECORD    | X         | GRANTED     | 1                      |
    |                  2990 |        61 |      372 | PRIMARY    | RECORD    | X         | GRANTED     | 2                      |
    |                  2990 |        61 |      372 | PRIMARY    | RECORD    | X         | GRANTED     | 3                      |
    |                  2990 |        61 |      372 | PRIMARY    | RECORD    | X         | GRANTED     | 4                      |
    |                  2990 |        61 |      372 | PRIMARY    | RECORD    | X         | GRANTED     | 5                      |
    |                  2990 |        61 |      372 | PRIMARY    | RECORD    | X         | GRANTED     | 6                      |
    +-----------------------+-----------+----------+------------+-----------+-----------+-------------+------------------------+
    ``` 
##### &emsp;5) 总结&分析
- 加锁情况分析 y:为比较值 x:y前面一条记录 z:y后面一条记录
  
    /|=|=不存在|<|<=|">"|">="
    ---|---|---|---|---|---|---
    主键|y+record|z+gap|y+gap|y+next-key|z+next-key|z+next-key&y+record
    唯一键|y+record|z+gap|y+next-key|y+next-key&z+gap|z+next-key|z+next-key&y+next-key
    非唯一键|y+next-key&z+gap|z+gap|y+next-key|y+next-key&z+next-key|z+next-key|z+next-key&y+next-key
      
### 2.insert

- 说明
    Insert的加锁方式是implict lock,分记录上有锁和无两种情况。

##### &emsp;1) 无锁
- 举个栗子
    ```
    insert into stu(no,name,remark) values(7,'X','X');
    ```
- 加锁情况
    ```
    +-----------------------+-----------+----------+------------+-----------+-----------+-------------+-----------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE | LOCK_STATUS | LOCK_DATA |
    +-----------------------+-----------+----------+------------+-----------+-----------+-------------+-----------+
    |                  2991 |        61 |      375 | NULL       | TABLE     | IX        | GRANTED     | NULL      |
    +-----------------------+-----------+----------+------------+-----------+-----------+-------------+-----------+
    ```

- duplicate key表现
    ```
    commit;
    insert into stu(no,name,remark) values(7,'X','X');
    
    RROR 1062 (23000): Duplicate entry '7' for key 'stu.uidx_no'
    ```  
        
##### &emsp; 2) 有锁    
- 举个栗子
    
    time|session1|session2|session3|session4|session5
    ---|---|---|---|---|---
    t1|select * from stu where name = 'X' for update;| | |
    t2| |select * from stu where no = 7 for update;| | | 
    t3| | |select * from stu where id > 7 for update;| | |
    t4| | | |insert into stu(no,name,remark) values(7,'X','X');| 
    t5| | |commit;| |
    t6| |commit;| | |
    t7|commit;| | | |
    t8| | | | |insert into stu(no,name,remark) values(7,'X','X');
    t9| | | |rollback;|
    t10|select * from stu where no = 7 for update;| | | |
    t11| |select * from stu where id = 18 for update;| | |
    t12| | | | |commit;

- 各时刻锁情况
    - t4   
    ```
    +-----------------------+-----------+----------+------------+-----------+--------------------+-------------+------------------------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE          | LOCK_STATUS | LOCK_DATA              |
    +-----------------------+-----------+----------+------------+-----------+--------------------+-------------+------------------------+
    |                  3024 |        61 |      402 | NULL       | TABLE     | IX                 | GRANTED     | NULL                   |
    |                  3024 |        61 |      402 | idx_name   | RECORD    | X                  | GRANTED     | supremum pseudo-record |
    |                  3025 |        62 |      123 | NULL       | TABLE     | IX                 | GRANTED     | NULL                   |
    |                  3025 |        62 |      123 | uidx_no    | RECORD    | X,GAP              | GRANTED     | 10, 2                  |
    |                  3026 |        65 |       86 | NULL       | TABLE     | IX                 | GRANTED     | NULL                   |
    |                  3026 |        65 |       86 | PRIMARY    | RECORD    | X                  | GRANTED     | supremum pseudo-record |
    |                  3027 |        63 |       92 | NULL       | TABLE     | IX                 | GRANTED     | NULL                   |
    |                  3027 |        63 |       92 | PRIMARY    | RECORD    | X,INSERT_INTENTION | WAITING     | supremum pseudo-record |
    +-----------------------+-----------+----------+------------+-----------+--------------------+-------------+------------------------+
    ```
    - t5
    ```
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+------------------------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE              | LOCK_STATUS | LOCK_DATA              |
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+------------------------+
    |                  3028 |        61 |      405 | NULL       | TABLE     | IX                     | GRANTED     | NULL                   |
    |                  3028 |        61 |      405 | idx_name   | RECORD    | X                      | GRANTED     | supremum pseudo-record |
    |                  3029 |        62 |      127 | NULL       | TABLE     | IX                     | GRANTED     | NULL                   |
    |                  3029 |        62 |      127 | uidx_no    | RECORD    | X,GAP                  | GRANTED     | 10, 2                  |
    |                  3031 |        63 |       95 | NULL       | TABLE     | IX                     | GRANTED     | NULL                   |
    |                  3031 |        63 |       95 | PRIMARY    | RECORD    | X,INSERT_INTENTION     | GRANTED     | supremum pseudo-record |
    |                  3031 |        63 |       95 | uidx_no    | RECORD    | X,GAP,INSERT_INTENTION | WAITING     | 10, 2                  |
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+------------------------+
    ```   
    - t6
    ```
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+------------------------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE              | LOCK_STATUS | LOCK_DATA              |
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+------------------------+
    |                  3028 |        61 |      405 | NULL       | TABLE     | IX                     | GRANTED     | NULL                   |
    |                  3028 |        61 |      405 | idx_name   | RECORD    | X                      | GRANTED     | supremum pseudo-record |
    |                  3031 |        63 |       95 | NULL       | TABLE     | IX                     | GRANTED     | NULL                   |
    |                  3031 |        63 |       95 | PRIMARY    | RECORD    | X,INSERT_INTENTION     | GRANTED     | supremum pseudo-record |
    |                  3031 |        63 |       95 | uidx_no    | RECORD    | X,GAP,INSERT_INTENTION | GRANTED     | 10, 2                  |
    |                  3031 |        63 |       95 | idx_name   | RECORD    | X,INSERT_INTENTION     | WAITING     | supremum pseudo-record |
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+------------------------+
    ``` 
    - t7
    ```
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+------------------------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE              | LOCK_STATUS | LOCK_DATA              |
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+------------------------+
    |                  3031 |        63 |       95 | NULL       | TABLE     | IX                     | GRANTED     | NULL                   |
    |                  3031 |        63 |       95 | PRIMARY    | RECORD    | X,INSERT_INTENTION     | GRANTED     | supremum pseudo-record |
    |                  3031 |        63 |       95 | uidx_no    | RECORD    | X,GAP,INSERT_INTENTION | GRANTED     | 10, 2                  |
    |                  3031 |        63 |       95 | idx_name   | RECORD    | X,INSERT_INTENTION     | GRANTED     | supremum pseudo-record |
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+------------------------+
    ``` 
    - t8
    ```
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+------------------------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE              | LOCK_STATUS | LOCK_DATA              |
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+------------------------+
    |                  3031 |        63 |       95 | NULL       | TABLE     | IX                     | GRANTED     | NULL                   |
    |                  3031 |        63 |       95 | PRIMARY    | RECORD    | X,INSERT_INTENTION     | GRANTED     | supremum pseudo-record |
    |                  3031 |        63 |       95 | uidx_no    | RECORD    | X,GAP,INSERT_INTENTION | GRANTED     | 10, 2                  |
    |                  3031 |        61 |      408 | uidx_no    | RECORD    | X,REC_NOT_GAP          | GRANTED     | 7, 17                  |
    |                  3031 |        63 |       95 | idx_name   | RECORD    | X,INSERT_INTENTION     | GRANTED     | supremum pseudo-record |
    |                  3036 |        61 |      408 | NULL       | TABLE     | IX                     | GRANTED     | NULL                   |
    |                  3036 |        61 |      408 | uidx_no    | RECORD    | S                      | WAITING     | 7, 17                  |
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+------------------------+
    ```
    - t9
    ```
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE              | LOCK_STATUS | LOCK_DATA |
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+-----------+
    |                  3036 |        61 |      408 | NULL       | TABLE     | IX                     | GRANTED     | NULL      |
    |                  3036 |        63 |       96 | uidx_no    | RECORD    | S,GAP                  | GRANTED     | 10, 2     |
    |                  3036 |        61 |      408 | uidx_no    | RECORD    | X,GAP,INSERT_INTENTION | GRANTED     | 10, 2     |
    |                  3036 |        63 |       96 | uidx_no    | RECORD    | S,GAP                  | GRANTED     | 7, 18     |
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+-----------+
    ```
    - t10
    ```
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+-----------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE              | LOCK_STATUS | LOCK_DATA |
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+-----------+
    |                  3036 |        61 |      408 | NULL       | TABLE     | IX                     | GRANTED     | NULL      |
    |                  3036 |        63 |       96 | uidx_no    | RECORD    | S,GAP                  | GRANTED     | 10, 2     |
    |                  3036 |        61 |      408 | uidx_no    | RECORD    | X,GAP,INSERT_INTENTION | GRANTED     | 10, 2     |
    |                  3036 |        63 |       96 | uidx_no    | RECORD    | S,GAP                  | GRANTED     | 7, 18     |
    |                  3036 |        62 |      130 | uidx_no    | RECORD    | X,REC_NOT_GAP          | GRANTED     | 7, 18     |
    |                  3037 |        62 |      130 | NULL       | TABLE     | IX                     | GRANTED     | NULL      |
    |                  3037 |        62 |      131 | uidx_no    | RECORD    | X,REC_NOT_GAP          | WAITING     | 7, 18     |
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+-----------+
    ```
    - t11
    ```
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+-----------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE              | LOCK_STATUS | LOCK_DATA |
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+-----------+
    |                  3036 |        61 |      408 | NULL       | TABLE     | IX                     | GRANTED     | NULL      |
    |                  3036 |        65 |       93 | PRIMARY    | RECORD    | X,REC_NOT_GAP          | GRANTED     | 18        |
    |                  3036 |        63 |       96 | uidx_no    | RECORD    | S,GAP                  | GRANTED     | 10, 2     |
    |                  3036 |        61 |      408 | uidx_no    | RECORD    | X,GAP,INSERT_INTENTION | GRANTED     | 10, 2     |
    |                  3036 |        63 |       96 | uidx_no    | RECORD    | S,GAP                  | GRANTED     | 7, 18     |
    |                  3036 |        62 |      130 | uidx_no    | RECORD    | X,REC_NOT_GAP          | GRANTED     | 7, 18     |
    |                  3037 |        62 |      130 | NULL       | TABLE     | IX                     | GRANTED     | NULL      |
    |                  3037 |        62 |      131 | uidx_no    | RECORD    | X,REC_NOT_GAP          | WAITING     | 7, 18     |
    |                  3038 |        65 |       93 | NULL       | TABLE     | IX                     | GRANTED     | NULL      |
    |                  3038 |        65 |       93 | PRIMARY    | RECORD    | X,REC_NOT_GAP          | WAITING     | 18        |
    +-----------------------+-----------+----------+------------+-----------+------------------------+-------------+-----------+
    ```
    - t12
    ```
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    |                  3037 |        62 |      130 | NULL       | TABLE     | IX            | GRANTED     | NULL      |
    |                  3037 |        62 |      131 | PRIMARY    | RECORD    | X,REC_NOT_GAP | WAITING     | 18        |
    |                  3037 |        62 |      131 | uidx_no    | RECORD    | X,REC_NOT_GAP | GRANTED     | 7, 18     |
    |                  3038 |        65 |       93 | NULL       | TABLE     | IX            | GRANTED     | NULL      |
    |                  3038 |        65 |       93 | PRIMARY    | RECORD    | X,REC_NOT_GAP | GRANTED     | 18        |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    ```
##### &emsp; 3) 总结&分析

- Insert 使用implict方式加锁,只有冲突时才会加锁
- Insert 会按照主键->唯一键->普通索引的顺序依次检查对应的索引值是否可以加插入意向锁。  
### 3.delete
##### &emsp; 1) where语法块加锁规则:同上面where部分介绍
##### &emsp; 2) 加锁分析
- 举个栗子

    time|session1|session2
    ---|---|---
    t1|delete from stu where id = 18;| 
    t2| |select * from stu where name = 'X' for update;|
    t3|rollback;|rollback;|
    t4|delete from stu where id = 18;| 
    t5| |select * from stu where no = 7 for update;|

- 各时刻加锁情况    
    - t1
    ```
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    |                  3071 |        61 |      439 | NULL       | TABLE     | IX            | GRANTED     | NULL      |
    |                  3071 |        61 |      439 | PRIMARY    | RECORD    | X,REC_NOT_GAP | GRANTED     | 18        |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    ```    
    - t2
    ```
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    |                  3071 |        61 |      439 | NULL       | TABLE     | IX            | GRANTED     | NULL      |
    |                  3071 |        61 |      439 | PRIMARY    | RECORD    | X,REC_NOT_GAP | GRANTED     | 18        |
    |                  3071 |        62 |      144 | idx_name   | RECORD    | X,REC_NOT_GAP | GRANTED     | 'X', 18   |
    |                  3076 |        62 |      144 | NULL       | TABLE     | IX            | GRANTED     | NULL      |
    |                  3076 |        62 |      144 | idx_name   | RECORD    | X             | WAITING     | 'X', 18   |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    ``` 
    - t5
    ```
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | EVENT_ID | INDEX_NAME | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    |                  3076 |        62 |      144 | NULL       | TABLE     | IX            | GRANTED     | NULL      |
    |                  3076 |        62 |      145 | uidx_no    | RECORD    | X             | WAITING     | 7, 18     |
    |                  3078 |        61 |      442 | NULL       | TABLE     | IX            | GRANTED     | NULL      |
    |                  3078 |        61 |      442 | PRIMARY    | RECORD    | X,REC_NOT_GAP | GRANTED     | 18        |
    |                  3078 |        62 |      145 | uidx_no    | RECORD    | X,REC_NOT_GAP | GRANTED     | 7, 18     |
    +-----------------------+-----------+----------+------------+-----------+---------------+-------------+-----------+
    ```
##### &emsp; 3) 总结&分析
- delete与insert类似:需要删除记录的索引加记录锁/next key锁，并且也是隐式锁。         

### 4.update
##### &emsp; 1) where语法块加锁规则:同上面where部分介绍
##### &emsp; 2) set语法块加锁分析

- 栗子

    time|session1|session2|session3
    ---|---|---|---
    t1|update stu set no = 2 where id = 18;| 
    t2| |select * from stu where no = 7 for update;|
    t3| | |insert into stu(no,name,remark) values(2,'X','XXX');

- 各时刻加锁情况
    
    - t1
    ```
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+-----------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | INDEX_NAME | OBJECT_INSTANCE_BEGIN | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA |
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+-----------+
    |                  4133 |        49 | NULL       |       140683790855864 | TABLE     | IX            | GRANTED     | NULL      |
    |                  4133 |        49 | PRIMARY    |       140683633655832 | RECORD    | X,REC_NOT_GAP | GRANTED     | 18        |
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+-----------+
    ```
    - t2 
    ```
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+-----------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | INDEX_NAME | OBJECT_INSTANCE_BEGIN | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA |
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+-----------+
    |                  4134 |        51 | NULL       |       140683790857864 | TABLE     | IX            | GRANTED     | NULL      |
    |                  4134 |        51 | uidx_no    |       140683633660440 | RECORD    | X             | WAITING     | 7, 18     |
    |                  4133 |        49 | NULL       |       140683790855864 | TABLE     | IX            | GRANTED     | NULL      |
    |                  4133 |        49 | PRIMARY    |       140683633655832 | RECORD    | X,REC_NOT_GAP | GRANTED     | 18        |
    |                  4133 |        51 | uidx_no    |       140683633656176 | RECORD    | X,REC_NOT_GAP | GRANTED     | 7, 18     |
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+-----------+
    ```  
    - t3
    ```
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+-----------+
    | ENGINE_TRANSACTION_ID | THREAD_ID | INDEX_NAME | OBJECT_INSTANCE_BEGIN | LOCK_TYPE | LOCK_MODE     | LOCK_STATUS | LOCK_DATA |
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+-----------+
    |                  4143 |        52 | NULL       |       140683790863832 | TABLE     | IX            | GRANTED     | NULL      |
    |                  4143 |        52 | uidx_no    |       140683633674264 | RECORD    | S             | WAITING     | 2, 18     |
    |                  4142 |        51 | NULL       |       140683790857864 | TABLE     | IX            | GRANTED     | NULL      |
    |                  4142 |        51 | uidx_no    |       140683633660440 | RECORD    | X             | WAITING     | 7, 18     |
    |                  4137 |        49 | NULL       |       140683790855864 | TABLE     | IX            | GRANTED     | NULL      |
    |                  4137 |        49 | PRIMARY    |       140683633655832 | RECORD    | X,REC_NOT_GAP | GRANTED     | 18        |
    |                  4137 |        51 | uidx_no    |       140683633656176 | RECORD    | X,REC_NOT_GAP | GRANTED     | 2, 18     |
    |                  4137 |        51 | uidx_no    |       140683633656176 | RECORD    | X,REC_NOT_GAP | GRANTED     | 7, 18     |
    +-----------------------+-----------+------------+-----------------------+-----------+---------------+-------------+-----------+
    ```     

##### &emsp;3) 总结&分析
- set部分的加锁规则与insert和delete类似。    
    
### 5.insert on duplicate key update
- 当没有唯一约束冲突时,加锁情况同insert
- 当有唯一约束冲突时,加锁同update

## 四.不同数据库版本问题
### 1.insert & insert on duplicate key update
- 8.0.19&5.7.30版本:按照implict lock的加锁规则。
- 5.7.18&5.7.25版本:对于唯一键insert使用explict的方式加record锁，insert on duplicate key update使用explict的方式加next-key锁，下面的死锁case就是在5.7.18&5.7.25这两个版本重现的。

## 五.死锁
### 1.case1
- 触发场景    

    time/session|session1|session2
    ---|---|---
    t1|insert into stu(no,name) values(2,'x') on duplicate key update name=values(name);| 
    t2| |insert into stu(no,name) values(3,'x') on duplicate key update name=values(name);
    t3|insert into stu(no,name) values(4,'x') on duplicate key update name=values(name);| 
- 总结&分析
    - t2时刻session2会被阻塞住，t3时刻session2直接报deadlock异常。
    - 分析，t1时session1在唯一索引uidx_no(,5)加gap,t2时session2尝试在唯一索引uidx_no(2,5)加gap和插入意向锁,所以session2被block住，t3时session1尝试在唯一索引uidx_no(2,5)加gap和插入意向锁。
    - 在极限情况下只需要两条insert语句就会产生死锁:session1和session2同时在(,5)上加了gap然后又同时去申请插入意向锁。PS:这个case我们线上遇到过，并发高的时候产生大量死锁，把事务隔离级别改成read commit可以解决问题。  