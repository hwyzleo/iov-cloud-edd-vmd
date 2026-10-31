package net.hwyz.iov.cloud.edd.vmd.service.timeit;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.BusinessKeyProvisioningConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VmdOutboxConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.BusinessKeyProvisioningPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VmdOutboxPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CR-073 G1 MapStruct 时间转换对账（TC-TIME-05 子集，用户确认第 6 点）。
 * <p>
 * 实测生成代码（MapStruct 1.4.2.Final）：{@code LocalDateTime↔java.util.Date} 转换固定 UTC：
 * <ul>
 *   <li>Domain(LDT)→PO(Date)：{@code Date.from(ldt.toInstant(ZoneOffset.UTC))}</li>
 *   <li>PO(Date)→Domain(LDT)：{@code LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"))}</li>
 * </ul>
 * 与 CR-073 报告早期推断（systemDefault）不一致——本测试固化「固定 UTC」事实；
 * 生产 converter 与调用方保持不动，不修改任何转换器。
 */
@DisplayName("CR-073 G1 MapStruct LDT↔Date 转换对账（不改转换器）")
class MapStructTimeConversionIT {

    @Test
    @DisplayName("VmdOutboxConverter：领域 LDT createTime → PO Date 固定按 UTC 解释")
    void vmdOutboxConverter_domainLdtToPoDate_isUtc() {
        LocalDateTime ldt = LocalDateTime.of(2026, 10, 30, 14, 0, 0);
        VmdOutbox entity = VmdOutbox.builder().eventId("e").createTime(ldt).build();
        VmdOutboxPo po = VmdOutboxConverter.INSTANCE.toPo(entity);
        assertEquals(ldt.toInstant(ZoneOffset.UTC), po.getCreateTime().toInstant(),
                "MapStruct 固定 UTC：LDT 14:00 → Date 14:00Z（非 JVM 默认区）");
        // 对照：若按 JVM 默认区解释会得到不同结果（记录证据）
        if (!ZoneId.systemDefault().equals(ZoneId.of("UTC"))) {
            System.out.println("[CR073-G1][记录] MapStruct 固定 UTC 与 systemDefault(" + ZoneId.systemDefault()
                    + ") 解释差异 = "
                    + (ldt.toInstant(ZoneOffset.UTC).toEpochMilli()
                    - ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()) / 3_600_000 + "h");
        }
    }

    @Test
    @DisplayName("VmdOutboxConverter：PO Date → 领域 LDT 固定按 UTC 还原（非 JVM 默认区）")
    void vmdOutboxConverter_poDateToDomainLdt_isUtc() {
        java.util.Date date = java.util.Date.from(java.time.Instant.parse("2026-10-30T06:00:00Z"));
        VmdOutboxPo po = VmdOutboxPo.builder().eventId("e").createTime(date).build();
        VmdOutbox entity = VmdOutboxConverter.INSTANCE.toEntity(po);
        assertEquals(LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")), entity.getCreateTime(),
                "MapStruct 固定 UTC：Date 06:00Z → LDT 06:00（UTC 墙上）");
    }

    @Test
    @DisplayName("BusinessKeyProvisioningConverter：领域 LDT audit → PO Date 固定 UTC（业务 key 域）")
    void businessKeyConverter_auditTimes_isUtc() {
        LocalDateTime modify = LocalDateTime.of(2026, 10, 30, 15, 30, 45, 123_456_000);
        BusinessKeyProvisioning domain = BusinessKeyProvisioning.builder()
                .requestId("rid")
                .deviceSn("sn")
                .businessDomain("V2C_COMM")
                .status(net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState.PREPARING)
                .createTime(modify)
                .modifyTime(modify)
                .build();
        BusinessKeyProvisioningPo po = BusinessKeyProvisioningConverter.INSTANCE.fromDomain(domain);
        // java.util.Date 仅毫秒精度：LDT 微秒 → Date 必然截断到毫秒（BasePo 审计承载精度证据）
        java.time.Instant expectedMillis = modify.toInstant(ZoneOffset.UTC)
                .truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        assertEquals(expectedMillis, po.getCreateTime().toInstant(),
                "业务 key 域 MapStruct 同样固定 UTC（生成代码 ZoneOffset.UTC）；Date 毫秒精度截断为预期");
        assertEquals(expectedMillis, po.getModifyTime().toInstant());
        // 往返：PO → Domain 还原为 UTC 墙上（毫秒）
        BusinessKeyProvisioning back = BusinessKeyProvisioningConverter.INSTANCE.toDomain(po);
        assertEquals(LocalDateTime.ofInstant(expectedMillis, ZoneId.of("UTC")),
                back.getModifyTime(), "PO→Domain 固定 UTC 还原（毫秒）");
    }
}
