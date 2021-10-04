package com.kj.repo.demo.algorithm;

/**
 * An object that generates IDs. This is broken into a separate class in case we
 * ever want to support multiple worker threads per process
 * sequence:9位，单实例512op/ms
 * workerId:9位，512实例
 * datacenter:4位，16机房
 * timestamp:40位，34年
 */
public class Snowflake {
    private static final long TWEPOCH = 1585670400000L;


    private final long bitDatacenterId = 4L;
    private final long bitWorkerId = 9L;
    private final long bitSequence = 10L;

    private final long maxDatacenterId = ~(Long.MAX_VALUE << bitDatacenterId);
    private final long maxWorkerId = ~(Long.MAX_VALUE << bitWorkerId);
    private final long maxSequence = ~(Long.MAX_VALUE << bitSequence);

    private final long shiftWorkerId = bitSequence;
    private final long shiftDatacenterId = bitSequence + bitWorkerId;
    private final long shiftTimestamp = bitSequence + bitWorkerId + bitDatacenterId;

    private final long datacenterId;
    private final long workerId;

    private long sequence = 0L;
    private long lastSequence = 0L;
    private long lastTimestamp = -1L;

    public Snowflake(long datacenterId, long workerId) {
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(
                    String.format("datacenterId can't be greater than %d or less than 0", maxDatacenterId));
        }
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("workerId can't be greater than %d or less than 0", maxWorkerId));
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    public synchronized long next() {
        long timestamp = timeGen();
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & maxSequence;
            if (sequence == lastSequence) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else if (lastTimestamp < timestamp) {
            lastTimestamp = timestamp;
            lastSequence = sequence;
            sequence = (sequence + 1) & maxSequence;
        } else {
            throw new RuntimeException(String.format(
                    "Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }
        return ((timestamp - TWEPOCH) << shiftTimestamp) | (datacenterId << shiftDatacenterId)
                | (workerId << shiftWorkerId) | sequence;
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