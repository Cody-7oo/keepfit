package com.example.demo.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 雪花算法分布式ID生成器（企业标准）
 */
@Slf4j
@Component
public class SnowflakeIdUtil {

    // 起始时间戳 2025-01-01 00:00:00
    private static final long EPOCH = 1735689600000L;

    // 机器ID 位数
    private static final long WORKER_ID_BITS = 5L;

    // 数据中心ID 位数
    private static final long DATA_CENTER_ID_BITS = 5L;

    // 序列号 位数
    private static final long SEQUENCE_BITS = 12L;

    // 最大机器ID
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    // 最大数据中心ID
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

    // 机器ID左移位数
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    // 数据中心ID左移位数
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    // 时间戳左移位数
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    // 序列号掩码
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private final long workerId;
    private final long dataCenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdUtil() {
        this.workerId = 1L;
        this.dataCenterId = 1L;
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("机器ID不能大于 " + MAX_WORKER_ID + " 或小于 0");
        }
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException("数据中心ID不能大于 " + MAX_DATA_CENTER_ID + " 或小于 0");
        }
    }

    public synchronized long nextId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            log.error("时钟回拨，拒绝生成ID");
            throw new RuntimeException("时钟回拨，无法生成ID");
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (dataCenterId << DATA_CENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }
}