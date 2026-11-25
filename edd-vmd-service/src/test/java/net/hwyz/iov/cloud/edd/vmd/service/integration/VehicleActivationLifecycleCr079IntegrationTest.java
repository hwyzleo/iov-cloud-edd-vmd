package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ActivationFactNormalized;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ActivationFactRecordMeta;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleActivationFactConsumeAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleLifecycleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.common.util.ActivationPayloadDigest;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleActivationNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ActivationDigestSource;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleLifecycleNodeEnum;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleActivationNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleLifecycleNodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 首次激活事实 → 既有生命周期节点集成测试（VMD-DSN-CR-079 / TC079-01～04、TC079-11）
 * <p>
 * 真实 MySQL + 真实事务验证：台账/投影/生命周期同事务写入、occurredAt 最早值收敛、
 * 重复投递不新增节点、未建档不写生命周期、US-052 只读时间线可见该节点。
 * 依赖本地 dev profile 的 MySQL/Nacos；不可用时按跳过/失败如实记录，不得据此写验收通过。
 *
 * @author hwyz_leo
 * @since 2026-11-25
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@DisplayName("CR-079 首次激活写入既有生命周期集成测试")
class VehicleActivationLifecycleCr079IntegrationTest {

    private static final String TOPIC = "tsp.user-vehicle-activation-fact";
    private static final ActivationFactRecordMeta META = new ActivationFactRecordMeta(TOPIC, 0, 1L);

    @Autowired
    private VehicleActivationFactConsumeAppService consumeAppService;

    @Autowired
    private VehicleLifecycleAppService vehicleLifecycleAppService;

    @Autowired
    private VehicleLifecycleNodeRepository lifecycleNodeRepository;

    @Autowired
    private VehicleActivationNodeRepository activationNodeRepository;

    @Autowired
    private VehBasicInfoRepository vehBasicInfoRepository;

    private ActivationFactNormalized fact(String vin, String eventId, String op, String occurredAt) {
        String digest = ActivationPayloadDigest.compute(vin, op, "vso:order:SO-CR079#delivery", occurredAt);
        return ActivationFactNormalized.builder()
                .eventId(eventId)
                .schemaVersion(1)
                .vin(vin)
                .bindingOperationId(op)
                .sourceFactRef("vso:order:SO-CR079#delivery")
                .occurredAt(Instant.parse(occurredAt))
                .occurredAtText(occurredAt)
                .payloadDigest(digest)
                .localDigest(digest)
                .digestSource(ActivationDigestSource.LOCAL)
                .digestProvidedButMismatch(false)
                .build();
    }

    private void givenVehicle(String vin) {
        vehBasicInfoRepository.insert(VehicleBasicInfo.builder()
                .vin(vin)
                .plantCode("IT")
                .brandCode("IT")
                .platformCode("IT")
                .carLineCode("IT")
                .modelCode("IT")
                .variantCode("IT")
                .configurationCode("IT")
                .build());
    }

    private static Instant truncatedToSeconds(LocalDateTime utc) {
        return utc.toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }

    @Test
    @DisplayName("TC079-01：已建档合法事实 → 台账/投影/VEHICLE_ACTIVE 同事务，US-052 可见且时间取 occurredAt")
    void writesLifecycleAndVisibleInTimeline() {
        String vin = "CR079IT0000000001";
        givenVehicle(vin);
        Instant occurredAt = Instant.parse("2026-11-22T09:45:01Z");

        consumeAppService.consume(fact(vin, "CR079-EVT-1", "CR079-BOP-1", "2026-11-22T09:45:01Z"), META);

        VehicleActivationNode projection = activationNodeRepository.selectByVin(vin);
        assertNotNull(projection, "激活投影应存在");
        assertEquals(1, projection.getVinResolved(), "已建档 VIN 应标记已解析");

        VehicleLifecycleNode lifecycle = lifecycleNodeRepository
                .selectByVinAndNode(vin, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE);
        assertNotNull(lifecycle, "生命周期 VEHICLE_ACTIVE 应写入");
        assertEquals(occurredAt, lifecycle.getReachTime().truncatedTo(ChronoUnit.SECONDS),
                "reachTime 必须取事实 occurredAt，不得使用接收/消费时间");

        List<VehicleLifecycleNode> timeline = vehicleLifecycleAppService.getVehicleTimelineByVin(vin);
        assertTrue(timeline.stream().anyMatch(node -> node.getNode() == VehicleLifecycleNodeEnum.VEHICLE_ACTIVE),
                "US-052 既有只读时间线应原样返回该节点");
        assertFalse(timeline.isEmpty());
    }

    @Test
    @DisplayName("TC079-03：乱序更早事实 → 生命周期仅向前纠正；更晚事实不前移")
    void outOfOrderFactsConvergeToEarliest() {
        String vin = "CR079IT0000000002";
        givenVehicle(vin);
        consumeAppService.consume(fact(vin, "CR079-EVT-2A", "CR079-BOP-2A", "2026-11-22T09:45:01Z"), META);
        consumeAppService.consume(fact(vin, "CR079-EVT-2B", "CR079-BOP-2B", "2026-11-20T01:00:00Z"), META);

        VehicleLifecycleNode corrected = lifecycleNodeRepository
                .selectByVinAndNode(vin, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE);
        assertEquals(Instant.parse("2026-11-20T01:00:00Z"),
                corrected.getReachTime().truncatedTo(ChronoUnit.SECONDS),
                "更早事实应向前纠正");
        assertEquals(Instant.parse("2026-11-20T01:00:00Z"),
                truncatedToSeconds(activationNodeRepository.selectByVin(vin).getFirstActivatedAt()));

        // 更晚事实：不覆盖既有最早时间
        consumeAppService.consume(fact(vin, "CR079-EVT-2C", "CR079-BOP-2C", "2026-11-25T01:00:00Z"), META);
        VehicleLifecycleNode unchanged = lifecycleNodeRepository
                .selectByVinAndNode(vin, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE);
        assertEquals(Instant.parse("2026-11-20T01:00:00Z"),
                unchanged.getReachTime().truncatedTo(ChronoUnit.SECONDS),
                "晚到事实不得前移首次激活时间");
    }

    @Test
    @DisplayName("TC079-02：同业务键重投 → VEHICLE_ACTIVE 节点唯一且时间不变")
    void duplicateDoesNotCreateSecondNode() {
        String vin = "CR079IT0000000003";
        givenVehicle(vin);
        consumeAppService.consume(fact(vin, "CR079-EVT-3A", "CR079-BOP-3A", "2026-11-22T09:45:01Z"), META);
        consumeAppService.consume(fact(vin, "CR079-EVT-3B", "CR079-BOP-3A", "2026-11-22T09:45:01Z"), META);

        List<VehicleLifecycleNode> nodes = lifecycleNodeRepository.selectByVin(vin);
        long activeCount = nodes.stream()
                .filter(node -> node.getNode() == VehicleLifecycleNodeEnum.VEHICLE_ACTIVE)
                .count();
        assertEquals(1L, activeCount, "同业务键重投不得新增第二个生命周期节点");
        assertEquals(Instant.parse("2026-11-22T09:45:01Z"),
                nodes.stream().filter(node -> node.getNode() == VehicleLifecycleNodeEnum.VEHICLE_ACTIVE)
                        .findFirst().orElseThrow().getReachTime().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("TC079-07：未建档 VIN → 只留投影 vin_resolved=0，不写生命周期")
    void unresolvedVinDoesNotWriteLifecycle() {
        String vin = "CR079IT0000000004";

        consumeAppService.consume(fact(vin, "CR079-EVT-4A", "CR079-BOP-4A", "2026-11-22T09:45:01Z"), META);

        VehicleActivationNode projection = activationNodeRepository.selectByVin(vin);
        assertNotNull(projection);
        assertEquals(0, projection.getVinResolved(), "未建档 VIN 应保留 vin_resolved=0");
        List<VehicleLifecycleNode> nodes = lifecycleNodeRepository.selectByVin(vin);
        assertTrue(nodes.stream().noneMatch(
                        node -> node.getNode() == VehicleLifecycleNodeEnum.VEHICLE_ACTIVE),
                "未建档 VIN 不得写无车辆锚点的生命周期节点");

        // D079-05：补偿扫描/标记 SQL 真实可用（keyset 分页 + vin_resolved 更新）
        assertTrue(activationNodeRepository.countByVinResolved(0) >= 1L);
        List<VehicleActivationNode> unresolvedPage =
                activationNodeRepository.selectPageByVinResolved(0, null, 50);
        assertTrue(unresolvedPage.stream().anyMatch(node -> vin.equals(node.getVin())),
                "未建档投影应可被补偿任务分页扫描到");
        assertEquals(1, activationNodeRepository.updateVinResolved(projection.getId(), 1));
        assertEquals(1, activationNodeRepository.selectByVin(vin).getVinResolved());
    }
}
