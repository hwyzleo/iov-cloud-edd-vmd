package net.hwyz.iov.cloud.edd.vmd.service.timeit;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * CR-073 TC-TIME-02/03 合成样本（用户确认：整秒/毫秒/微秒/跨日/带 offset/无 offset/负 epoch/边界）。
 */
public record TimeSample(String name, Instant instant, LocalDateTime ldtWall, String note) {

    /** 无 offset 业务墙钟样本：业务给定的墙上值（不代表任何时区语义） */
    public static final LocalDateTime WALL = LocalDateTime.of(2026, 10, 30, 8, 0, 0, 123_456_000);

    public static TimeSample[] all() {
        return new TimeSample[]{
                new TimeSample("S1_WHOLE_SECOND", Instant.parse("2026-10-30T00:00:00Z"),
                        LocalDateTime.of(2026, 10, 30, 0, 0, 0), "整秒"),
                new TimeSample("S2_MILLI", Instant.parse("2026-10-30T00:00:00.123Z"),
                        LocalDateTime.of(2026, 10, 30, 0, 0, 0, 123_000_000), "毫秒"),
                new TimeSample("S3_MICRO", Instant.parse("2026-10-30T00:00:00.123456Z"),
                        LocalDateTime.of(2026, 10, 30, 0, 0, 0, 123_456_000), "微秒"),
                new TimeSample("S4_CROSS_DAY", Instant.parse("2026-10-29T23:59:59.999Z"),
                        LocalDateTime.of(2026, 10, 29, 23, 59, 59, 999_000_000), "跨日"),
                new TimeSample("S5_WITH_OFFSET", Instant.parse("2026-10-30T08:00:00+08:00"),
                        LocalDateTime.of(2026, 10, 30, 8, 0, 0), "带 offset（=00:00Z）"),
                new TimeSample("S6_NEG_EPOCH", Instant.parse("1969-12-31T23:59:59.500Z"),
                        LocalDateTime.of(1969, 12, 31, 23, 59, 59, 500_000_000),
                        "负 epoch（DATETIME 范围内；TIMESTAMP 1970-01-01 下限之外）"),
                new TimeSample("S7_TIMESTAMP_MAX", Instant.parse("2038-01-19T03:14:07Z"),
                        LocalDateTime.of(2038, 1, 19, 3, 14, 7),
                        "TIMESTAMP 上界（2038-01-19 03:14:07Z）"),
                new TimeSample("S8_BEYOND_TIMESTAMP_MAX", Instant.parse("2038-01-19T03:14:08Z"),
                        LocalDateTime.of(2038, 1, 19, 3, 14, 8),
                        "超 TIMESTAMP 上界 1 秒（DATETIME 可存）"),
        };
    }
}
