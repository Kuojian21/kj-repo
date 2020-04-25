#源码阅读:log4j2&logback及性能对比
## 1.logback
### 1) 流程图
- 初始化
- 日志打印
### 2) 解析方式
按照深度优先遍历的方式初始化配置，要注意配置顺序，eg:相关联的Appender 一定要在 Logger 之前。
### 3) 异步
配置 AsyncAppender(ch.qos.logback.classic.AsyncAppender),AsyncAppender 通过单线程 +ArrayBlockingQueue 的方式写入 OutputStream。
### 4) 参数介绍
##### 1) includeCallerData
AsyncAppender将LoggerEvent移交到异步线程时，是否带上StackTraceElement，如果需要打印调用类、方法、行号的话一定要带上，否则打印的是异步线程的。
##### 2) neverBlock
AsyncAppender的ArrayBlockingQueue队列满了时，是否阻塞，如果为true则丢弃LoggerEvent。
## 2.log4j2
### 1) 流程图
- 初始化
- 日志打印
### 2) 解析方式
按照广度优先遍历的方式解析配置文件，无需关注配置顺序。
### 3) 异步
支持配置AsyncAppender(org.apache.logging.log4j.core.appender.AsyncAppender)和AsyncLogger(org.apache.logging.log4j.core.async.AsyncLogger)两种方式,AsyncAppender的实现与logback的一样，AsyncLogger则是通过Disruptor+RingBuffer的方式写入OutputStream。
### 4) 发现bug一枚(https://github.com/apache/logging-log4j2/pull/352)
初始化AsyncLogger时忽略了includeLocation，导致将LogEvent移交到异步线程时丢失StackTraceElement，进而无法打印调用类、方法、行号。
## 3.性能对比
### 0) 测试环境
- 机器
```$xslt
MacBook Pro (16-inch, 2019)
2.6 GHz 6-Core Intel Core i7
16 GB 2667 MHz DDR4
固态硬盘
并发线程数:6
```
- 测试方法
```$xslt
logger.info("{}", System.currentTimeMillis());
```
### 1) 日志配置见文末
日志 | 吞吐量(ops/ms:每毫秒日志数)
---|---
log4jAsync | 195.403
log4jSync | 83.473
logbackAsync | 2760.593
logbackSync | 79.695

分析:logbackAsync吞吐量异常高，经排查当日志级别低于<=INFO或者neverBlock=true且队列满时则丢弃日志。
```$xslt
    #ch.qos.logback.core.AsyncAppenderBase.append
    protected void append(E eventObject) {
        if (isQueueBelowDiscardingThreshold() && isDiscardable(eventObject)) {
            return;
        }
        preprocess(eventObject);
        put(eventObject);
    }
    #ch.qos.logback.classic.AsyncAppender.isDiscardable
    protected boolean isDiscardable(ILoggingEvent event) {
        Level level = event.getLevel();
        return level.toInt() <= Level.INFO_INT;
    }
```
```$xslt
    #ch.qos.logback.core.AsyncAppenderBase.put
    private void put(E eventObject) {
        if (neverBlock) {
            blockingQueue.offer(eventObject);
        } else {
            putUninterruptibly(eventObject);
        }
    }
```
### 2) 将logback配置neverBlock改为false且日志级别改为error重新测试
日志 | 吞吐量(ops/ms)
---|---
logbackAsync | 143.544
分析:测试结果正常，但是发现log4j2并未输出日志位置，经排查由于log4j2自身bug(https://github.com/apache/logging-log4j2/pull/352)导致。
### 3) 将logback配置includeCallerData改为false
日志 | 吞吐量(ops/ms)
---|---
logbackAsync | 41.537
logbackSync | 72.701
分析：logbackAsync吞吐量异常低，经排查是对includeCallerData参数的理解有误(源于对一段注释的理解错误)，这个参数控制的是将ILoggingEvent移交给异步线程之前是否生成当前的异常堆栈,如果此时不生成，则在异步线程解析%F:%L参数时，会获取当时的堆栈，首先获取到的堆栈是错误的，其次堆栈获取涉及到用户调用与系统调用的切换，是一个比较耗时操作。而同步模式的性能瓶颈是在OutputStream.write的锁上，堆栈是在锁外面获取的。
- includeCallerData
```$xslt
    protected void preprocess(ILoggingEvent eventObject) {
        eventObject.prepareForDeferredProcessing();
        if (this.includeCallerData) {
            eventObject.getCallerData();
        }
    }
```
- FileOfCallerConverter&LineOfCallerConverter
```
    #ch.qos.logback.classic.pattern.FileOfCallerConverter.convert
    public String convert(ILoggingEvent le) {
        StackTraceElement[] cda = le.getCallerData();
        if (cda != null && cda.length > 0) {
            return cda[0].getFileName();
        } else {
            return CallerData.NA;
        }
    }
    #ch.qos.logback.classic.pattern.LineOfCallerConverter.convert
    public String convert(ILoggingEvent le) {
        StackTraceElement[] cda = le.getCallerData();
        if (cda != null && cda.length > 0) {
            return Integer.toString(cda[0].getLineNumber());
        } else {
            return CallerData.NA;
        }
    }
```
        

### 4）修改日志格式[%d{yyyy-MM-dd HH:mm:ss.SSS} %X{UUID} [%t] %-5p - %msg%n]
日志 | 吞吐量(ops/ms)
---|---
log4jAsync | 169.458
log4jSync | 126.969
logbackAsync | 123.514
logbackSync | 120.085

## 4.总结
- 多线程下是怎么保证日志输出不乱的:同步模式是在write之前加锁保证的，异步模式是通过队列缓冲+单线程write保证的。
- log4j2与logback的同步模式效率差不多。
- log4j2的异步模式效率略高，这个应该是得益于Disruptor+RingBuffer算法的优势。
- 关于传言：打印行号会影响性能，这个确实是事实，但是如果已经打印了文件或方法名，再打印行号是不会影响性能的。
- log4j2与logback都提供了immediateFlush参数，控制每次write之后是否立即刷新，默认为true。
    - logback
    ```$xslt
    #ch.qos.logback.core.OutputStreamAppender.writeBytes
    private void writeBytes(byte[] byteArray) throws IOException {
        if(byteArray == null || byteArray.length == 0)
            return;
        
        lock.lock();
        try {
            this.outputStream.write(byteArray);
            if (immediateFlush) {
                this.outputStream.flush();
            }
        } finally {
            lock.unlock();
        }
    }
    ```
    - log4j2
    ```$xslt
    #org.apache.logging.log4j.core.appender.OutputStreamManager.write(byte[], int, int, boolean)
    protected synchronized void write(final byte[] bytes, final int offset, final int length, final boolean immediateFlush) {
        if (immediateFlush && byteBuffer.position() == 0) {
            writeToDestination(bytes, offset, length);
            flushDestination();
            return;
        }
        if (length >= byteBuffer.capacity()) {
            // if request length exceeds buffer capacity, flush the buffer and write the data directly
            flush();
            writeToDestination(bytes, offset, length);
        } else {
            if (length > byteBuffer.remaining()) {
                flush();
            }
            byteBuffer.put(bytes, offset, length);
        }
        if (immediateFlush) {
            flush();
        }
    }
    ```

## 5.配置示例
- logback.properties|logback-test.properties
  ```$xslt
  <?xml version="1.0" encoding="UTF-8"?>
  <configuration scan="false" scanPeriod="60 seconds" debug="false">
      <property name="LOG_HOME" value="${user.dir}/logs"/>
      <property name="LOG_FILE" value="benchmark-logback"/>
      <property name="logPattern"
                value="%d{yyyy-MM-dd HH:mm:ss.SSS} %X{UUID} [%t] %-5p \\(%F:%L\\) - %msg%n"></property>
      <appender name="stdout" class="ch.qos.logback.core.ConsoleAppender">
          <encoder charset="UTF-8">
              <pattern>${logPattern}</pattern>
              <prudent>true</prudent>
          </encoder>
      </appender>
  
      <!--Async:Please note the order-bg-->
      <appender name="asyncRef" class="ch.qos.logback.core.rolling.RollingFileAppender">
          <file>${LOG_HOME}/${LOG_FILE}.log</file>
          <immediateFlush>true</immediateFlush>
          <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
              <fileNamePattern>${LOG_HOME}/${LOG_FILE}.%d{yyyy-MM-dd}-%i.log</fileNamePattern>
              <maxHistory>${KJ_REPO_LOG_MAX_HISTORY:-15}</maxHistory>
              <maxFileSize>${KJ_REPO_LOG_MAX_FILE_SIZE:-500MB}</maxFileSize>
          </rollingPolicy>
          <encoder charset="UTF-8">
              <pattern>${logPattern}</pattern>
              <prudent>true</prudent>
          </encoder>
      </appender>
      <appender name="async" class="ch.qos.logback.classic.AsyncAppender">
          <queueSize>16384</queueSize>
          <neverBlock>true</neverBlock>
          <includeCallerData>true</includeCallerData>
          <appender-ref ref="asyncRef"/>
      </appender>
      <!--Async:Please note the order-ed-->
  
      <appender name="sync" class="ch.qos.logback.core.rolling.RollingFileAppender">
          <file>${LOG_HOME}/${LOG_FILE}-sync.log</file>
          <immediateFlush>true</immediateFlush>
          <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
              <fileNamePattern>${LOG_HOME}/${LOG_FILE}-sync.%d{yyyy-MM-dd}-%i.log</fileNamePattern>
              <maxHistory>${KJ_REPO_LOG_MAX_HISTORY:-15}</maxHistory>
              <maxFileSize>${KJ_REPO_LOG_MAX_FILE_SIZE:-500MB}</maxFileSize>
          </rollingPolicy>
          <encoder charset="UTF-8">
              <pattern>${logPattern}</pattern>
              <prudent>true</prudent>
          </encoder>
      </appender>
  
      <logger name="com.kj.repo.benchmark.logger" level="info" additivity="false">
          <!--<appender-ref ref="stdout"/>-->
          <appender-ref ref="async"/>
      </logger>
      <logger name="sync.com.kj.repo.benchmark.logger" level="info" additivity="false">
          <!--<appender-ref ref="stdout"/>-->
          <appender-ref ref="sync"/>
      </logger>
  
      <root level="error">
          <appender-ref ref="stdout"/>
          <appender-ref ref="sync"/>
      </root>
  </configuration> 
  ```
- log4j2.properties|log4j2-test.properties
  ```$xslt
  status=error
  name=kj-repo-4j2
  #property
  property.LOG_HOME=logs
  property.LOG_FILE=benchmark-log4j2
  filters=threshold
  filter.threshold.type=ThresholdFilter
  filter.threshold.level=info
  #appenders
  appenders=console,appender-async,appender-sync
  ##appender-console
  appender.console.name=console
  appender.console.type=Console
  appender.console.layout.type=PatternLayout
  appender.console.layout.pattern=%d{yyyy-MM-dd HH:mm:ss.SSS} %X{UUID} [%t] %-5p \\(%F:%L\\) - %msg%n
  appender.console.filter.threshold.type=ThresholdFilter
  appender.console.filter.threshold.level=info
  ##appender_appender-async
  appender.appender-async.name=4j2-appender-async
  appender.appender-async.type=RollingFile
  appender.appender-async.fileName=${LOG_HOME}/${LOG_FILE}.log
  appender.appender-async.filePattern=${LOG_HOME}/${LOG_FILE}-%d{yyyy-MM-dd}-%i.log
  appender.appender-async.layout.type=PatternLayout
  appender.appender-async.layout.pattern=%d{yyyy-MM-dd HH:mm:ss.SSS} %X{UUID} [%t] %-5p \\(%F:%L\\) - %msg%n
  appender.appender-async.policies.type=Policies
  appender.appender-async.policies.time.type=TimeBasedTriggeringPolicy
  appender.appender-async.policies.time.interval=1
  appender.appender-async.policies.time.modulate=true
  appender.appender-async.policies.size.type=SizeBasedTriggeringPolicy
  appender.appender-async.policies.size.size=500MB
  appender.appender-async.strategy.type=DefaultRolloverStrategy
  appender.appender-async.strategy.max=20
  appender.appender-async.filter.threshold.type=ThresholdFilter
  appender.appender-async.filter.threshold.level=info
  ##appender_appender-sync
  appender.appender-sync.name=4j2-appender-sync
  appender.appender-sync.type=RollingFile
  appender.appender-sync.fileName=${LOG_HOME}/${LOG_FILE}-sync.log
  appender.appender-sync.filePattern=${LOG_HOME}/${LOG_FILE}-sync-%d{yyyy-MM-dd}-%i.log
  appender.appender-sync.layout.type=PatternLayout
  appender.appender-sync.layout.pattern=%d{yyyy-MM-dd HH:mm:ss.SSS} %X{UUID} [%t] %-5p \\(%F:%L\\) - %msg%n
  appender.appender-sync.policies.type=Policies
  appender.appender-sync.policies.time.type=TimeBasedTriggeringPolicy
  appender.appender-sync.policies.time.interval=1
  appender.appender-sync.policies.time.modulate=true
  appender.appender-sync.policies.size.type=SizeBasedTriggeringPolicy
  appender.appender-sync.policies.size.size=500MB
  appender.appender-sync.strategy.type=DefaultRolloverStrategy
  appender.appender-sync.strategy.max=20
  appender.appender-sync.filter.threshold.type=ThresholdFilter
  appender.appender-sync.filter.threshold.level=info
  #loggers
  loggers=logger-async,logger-sync
  ##logger_logger-async
  logger.logger-async.name=com.kj.repo.benchmark.logger
  logger.logger-async.type=asyncLogger
  logger.logger-async.level=info
  logger.logger-async.additivity=false
  logger.logger-async.appenderRef.kj1.ref=4j2-appender-async
  ##logger_logger-sync
  logger.logger-sync.name=sync.com.kj.repo.benchmark.logger
  logger.logger-sync.level=info
  logger.logger-sync.additivity=false
  logger.logger-sync.appenderRef.kj1.ref=4j2-appender-sync
  #rootLogger
  rootLogger.level=info
  rootLogger.appenderRefs=kj1,kj2
  rootLogger.appenderRef.kj1.ref=4j2-appender-async
  ```