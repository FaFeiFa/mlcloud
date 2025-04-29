package com.hua.cloud.utils;

// ---------------------- 雪花算法生成器 ----------------------

import java.util.concurrent.atomic.AtomicLong;

/**
 * 雪花算法ID生成器 (简化线程安全版)
 * 结构：0 | 时间戳(41bit) | 数据中心ID(5bit) | 机器ID(5bit) | 序列号(12bit)
 */

public class SnowflakeIdGenerator {
    private final long datacenterId; // 数据中心ID (0-31)
    private final long machineId;    // 机器ID (0-31)
    private final AtomicLong lastTimestamp = new AtomicLong(-1L);
    private final AtomicLong sequence = new AtomicLong(0L);

    // 各部分的位长度
    private static final int SEQUENCE_BITS = 12;
    private static final int MACHINE_BITS = 5;
    private static final int DATACENTER_BITS = 5;

    // 最大取值
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_BITS);

    // 时间戳左移位数
    private static final int MACHINE_SHIFT = SEQUENCE_BITS;
    private static final int DATACENTER_SHIFT = SEQUENCE_BITS + MACHINE_BITS;
    private static final int TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_BITS + DATACENTER_BITS;

    public SnowflakeIdGenerator(long datacenterId, long machineId) {
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("数据中心ID范围: 0-" + MAX_DATACENTER_ID);
        }
        if (machineId > MAX_MACHINE_ID || machineId < 0) {
            throw new IllegalArgumentException("机器ID范围: 0-" + MAX_MACHINE_ID);
        }
        this.datacenterId = datacenterId;
        this.machineId = machineId;
    }

    public synchronized long nextId() {
        long currentTime = System.currentTimeMillis();

        // 时钟回拨检测
        if (currentTime < lastTimestamp.get()) {
            throw new RuntimeException("系统时钟回拨，拒绝生成ID");
        }

        // 同一毫秒内生成序列号
        if (currentTime == lastTimestamp.get()) {
            sequence.set((sequence.get() + 1) & MAX_SEQUENCE);
            if (sequence.get() == 0) { // 当前毫秒序列号用尽
                currentTime = waitNextMillis(currentTime);
            }
        } else {
            sequence.set(0L);
        }

        lastTimestamp.set(currentTime);

        // 组合各部分生成ID
        return ((currentTime) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_SHIFT)
                | (machineId << MACHINE_SHIFT)
                | sequence.get();
    }

    // 等待下一毫秒
    private long waitNextMillis(long currentTime) {
        long now;
        do {
            now = System.currentTimeMillis();
        } while (now <= currentTime);
        return now;
    }
}