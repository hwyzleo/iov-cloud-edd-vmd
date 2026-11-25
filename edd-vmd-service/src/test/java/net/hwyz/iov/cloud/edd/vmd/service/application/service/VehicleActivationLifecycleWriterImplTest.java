package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleLifecycleNodeEnum;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleLifecycleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.service.VehicleActivationLifecycleWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 首次激活事实 → 生命周期节点写入实现单元测试（VMD-DSN-CR-079 / D079-02、D079-03）
 * <p>
 * 覆盖 TC079-01/02/03/04：已建档插入、重复不动、乱序仅向前纠正、并发唯一键冲突重读重判、
 * 既有更早时间不被覆盖（无证据不前移）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleActivationLifecycleWriterImpl 测试")
class VehicleActivationLifecycleWriterImplTest {

    private static final String VIN = "LSVAA1234567890AB";
    private static final LocalDateTime EARLY = toLocal("2026-11-20T01:00:00.000Z");
    private static final LocalDateTime LATE = toLocal("2026-11-22T09:45:01.234Z");

    @Mock
    private VehicleLifecycleNodeRepository lifecycleNodeRepository;

    private VehicleActivationLifecycleWriterImpl writer;

    @BeforeEach
    void setUp() {
        writer = new VehicleActivationLifecycleWriterImpl(lifecycleNodeRepository);
    }

    private static LocalDateTime toLocal(String instant) {
        return LocalDateTime.ofInstant(Instant.parse(instant), ZoneOffset.UTC);
    }

    private VehicleLifecycleNode existing(Instant reachTime) {
        return VehicleLifecycleNode.builder()
                .id("7")
                .vin(VIN)
                .node(VehicleLifecycleNodeEnum.VEHICLE_ACTIVE)
                .reachTime(reachTime)
                .build();
    }

    @Test
    @DisplayName("TC079-01：节点不存在 → 插入 reachTime=事实最早时刻（APPLIED）")
    void insertWhenAbsent() {
        when(lifecycleNodeRepository.selectByVinAndNode(VIN, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE))
                .thenReturn(null);

        VehicleActivationLifecycleWriter.LifecycleWriteOutcome outcome = writer.write(VIN, EARLY);

        assertEquals(VehicleActivationLifecycleWriter.LifecycleWriteResult.APPLIED, outcome.result());
        assertNull(outcome.previousReachTime());
        assertEquals(EARLY.toInstant(ZoneOffset.UTC), outcome.currentReachTime());

        ArgumentCaptor<VehicleLifecycleNode> captor = ArgumentCaptor.forClass(VehicleLifecycleNode.class);
        verify(lifecycleNodeRepository).save(captor.capture());
        VehicleLifecycleNode inserted = captor.getValue();
        assertEquals(VIN, inserted.getVin());
        assertEquals(VehicleLifecycleNodeEnum.VEHICLE_ACTIVE, inserted.getNode());
        assertEquals(EARLY.toInstant(ZoneOffset.UTC), inserted.getReachTime());
        assertEquals(Integer.valueOf(0), inserted.getSort(), "既有生命周期节点 sort 口径不变（默认 0）");
        verify(lifecycleNodeRepository, never()).correctReachTimeIfLater(any(), any(), any());
    }

    @Test
    @DisplayName("TC079-02：节点已存在且时间更早 → 不动（DUPLICATE）")
    void keepEarlierExisting() {
        Instant existing = Instant.parse("2026-11-18T00:00:00.000Z");
        when(lifecycleNodeRepository.selectByVinAndNode(VIN, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE))
                .thenReturn(existing(existing));

        VehicleActivationLifecycleWriter.LifecycleWriteOutcome outcome = writer.write(VIN, EARLY);

        assertEquals(VehicleActivationLifecycleWriter.LifecycleWriteResult.DUPLICATE, outcome.result());
        assertEquals(existing, outcome.currentReachTime());
        verify(lifecycleNodeRepository, never()).save(any());
        verify(lifecycleNodeRepository, never()).correctReachTimeIfLater(any(), any(), any());
    }

    @Test
    @DisplayName("TC079-03：节点已存在且时间更晚 → 仅向前纠正（CORRECTED + 旧/新时间）")
    void correctForwardWhenExistingLater() {
        when(lifecycleNodeRepository.selectByVinAndNode(VIN, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE))
                .thenReturn(existing(LATE.toInstant(ZoneOffset.UTC)));
        when(lifecycleNodeRepository.correctReachTimeIfLater(
                VIN, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE, EARLY.toInstant(ZoneOffset.UTC)))
                .thenReturn(1);

        VehicleActivationLifecycleWriter.LifecycleWriteOutcome outcome = writer.write(VIN, EARLY);

        assertEquals(VehicleActivationLifecycleWriter.LifecycleWriteResult.CORRECTED, outcome.result());
        assertEquals(LATE.toInstant(ZoneOffset.UTC), outcome.previousReachTime());
        assertEquals(EARLY.toInstant(ZoneOffset.UTC), outcome.currentReachTime());
        verify(lifecycleNodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC079-03：既有节点 reach_time 为空 → 回填（不视为后退）")
    void fillNullReachTime() {
        when(lifecycleNodeRepository.selectByVinAndNode(VIN, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE))
                .thenReturn(existing(null));
        when(lifecycleNodeRepository.correctReachTimeIfLater(
                VIN, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE, EARLY.toInstant(ZoneOffset.UTC)))
                .thenReturn(1);

        VehicleActivationLifecycleWriter.LifecycleWriteOutcome outcome = writer.write(VIN, EARLY);

        assertEquals(VehicleActivationLifecycleWriter.LifecycleWriteResult.CORRECTED, outcome.result());
        assertNull(outcome.previousReachTime());
        assertEquals(EARLY.toInstant(ZoneOffset.UTC), outcome.currentReachTime());
    }

    @Test
    @DisplayName("TC079-03：条件更新 0 行（并发已收敛）→ 重读按 DUPLICATE")
    void zeroRowUpdate_treatedAsConverged() {
        Instant concurrent = Instant.parse("2026-11-19T00:00:00.000Z");
        when(lifecycleNodeRepository.selectByVinAndNode(VIN, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE))
                .thenReturn(existing(LATE.toInstant(ZoneOffset.UTC)), existing(concurrent));
        when(lifecycleNodeRepository.correctReachTimeIfLater(
                VIN, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE, EARLY.toInstant(ZoneOffset.UTC)))
                .thenReturn(0);

        VehicleActivationLifecycleWriter.LifecycleWriteOutcome outcome = writer.write(VIN, EARLY);

        assertEquals(VehicleActivationLifecycleWriter.LifecycleWriteResult.DUPLICATE, outcome.result());
        assertEquals(concurrent, outcome.currentReachTime());
    }

    @Test
    @DisplayName("TC079-04：插入唯一键冲突 → 重读后向前纠正（不误记失败）")
    void duplicateKeyOnInsert_rereadsAndCorrects() {
        when(lifecycleNodeRepository.selectByVinAndNode(VIN, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE))
                .thenReturn(null, existing(LATE.toInstant(ZoneOffset.UTC)));
        when(lifecycleNodeRepository.save(any(VehicleLifecycleNode.class)))
                .thenThrow(new DuplicateKeyException("uk_vin_node"));
        when(lifecycleNodeRepository.correctReachTimeIfLater(
                VIN, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE, EARLY.toInstant(ZoneOffset.UTC)))
                .thenReturn(1);

        VehicleActivationLifecycleWriter.LifecycleWriteOutcome outcome = writer.write(VIN, EARLY);

        assertEquals(VehicleActivationLifecycleWriter.LifecycleWriteResult.CORRECTED, outcome.result());
        assertEquals(LATE.toInstant(ZoneOffset.UTC), outcome.previousReachTime());
    }

    @Test
    @DisplayName("TC079-04：插入唯一键冲突且重读为空 → 原样抛出交事务回滚")
    void duplicateKeyOnInsert_rereadEmpty_rethrows() {
        when(lifecycleNodeRepository.selectByVinAndNode(VIN, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE))
                .thenReturn(null);
        when(lifecycleNodeRepository.save(any(VehicleLifecycleNode.class)))
                .thenThrow(new DuplicateKeyException("uk_vin_node"));

        assertThrows(DuplicateKeyException.class, () -> writer.write(VIN, EARLY));
    }

    @Test
    @DisplayName("参数非法（VIN 空 / 时间空）→ IllegalArgumentException")
    void invalidArgumentsRejected() {
        assertThrows(IllegalArgumentException.class, () -> writer.write(" ", EARLY));
        assertThrows(IllegalArgumentException.class, () -> writer.write(VIN, null));
    }
}
