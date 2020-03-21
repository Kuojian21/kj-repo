package com.kj.repo.infra.algorithm;

import org.slf4j.Logger;

import com.kj.repo.infra.logger.LoggerHelper;

/**
 * An object that generates IDs. This is broken into a separate class in case we
 * ever want to support multiple worker threads per process
 */
public class Snowflake {
    private static final Logger logger = LoggerHelper.getLogger();
    private final long workerIdBits = 5L;
    private final long datacenterIdBits = 5L;
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    private final long sequenceBits = 12L;
    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    private final long datacenterId;
    private final long workerId;
    private final long twepoch = 1557577014801L;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public Snowflake(long datacenterId, long workerId) {
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(
                    String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        logger.info(
                "worker starting. timestamp left shift %d, datacenter id bits %d,worker id bits %d, sequence bits %d,"
                        + " workerid %d",
                timestampLeftShift, datacenterIdBits, workerIdBits, sequenceBits, workerId);
        this.datacenterId = datacenterId;
        this.workerId = workerId;

    }

    public long getId() {
        return nextId();
    }

    protected synchronized long nextId() {
        long timestamp = timeGen();
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format(
                    "Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }
        lastTimestamp = timestamp;
        return ((timestamp - twepoch) << timestampLeftShift) | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift) | sequence;
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }

}