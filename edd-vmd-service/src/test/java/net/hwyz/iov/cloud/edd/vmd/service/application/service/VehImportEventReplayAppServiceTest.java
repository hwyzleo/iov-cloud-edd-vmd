package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ReplayVehicleImportEventCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ReplayEventResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleImportReplayPreview;
import net.hwyz.iov.cloud.edd.vmd.service.application.replay.VehicleImportReplayAction;
import net.hwyz.iov.cloud.edd.vmd.service.application.replay.VehicleImportReplayActionContext;
import net.hwyz.iov.cloud.edd.vmd.service.application.replay.VehicleImportReplayActionResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.replay.VehicleImportReplayActionRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.replay.VehicleImportReplayActionTarget;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.VehImportReplayExtractor;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleImportEventReplayActionNotFoundException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleImportEventReplayInProgressException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleImportEventReplayNotAllowedException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportEventReplay;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportEventReplayItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportEventReplayItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportEventReplayRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * 车辆导入事件补发编排应用服务单元测试
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发扩展为按 ImportType 路由的动作补偿
 * <p>
 * 覆盖：资格校验（类型/处理成功/原始数据）、preview 预检、replayId 幂等、
 * 执行中互斥、动作未登记异常、主任务 + 逐项动作审计、状态收敛、PRODUCE 兼容回归。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VehImportEventReplayAppService 测试")
class VehImportEventReplayAppServiceTest {

    @Mock
    private VehImportDataRepository vehImportDataRepository;
    @Mock
    private VmdOutboxRepository vmdOutboxRepository;

    private VehImportEventReplayAppService appService;
    private FakeMasterRepository masterRepository;
    private FakeItemRepository itemRepository;
    private VehicleImportReplayActionRegistry registry;

    /** 内存 Fake 主任务仓储 */
    private static class FakeMasterRepository implements VehImportEventReplayRepository {
        final Map<String, VehImportEventReplay> store = new LinkedHashMap<>();
        long seq = 1L;

        @Override
        public VehImportEventReplay selectById(Long id) {
            return store.values().stream().filter(r -> id.equals(r.getId())).findFirst().orElse(null);
        }

        @Override
        public VehImportEventReplay selectByReplayId(String replayId) {
            return store.get(replayId);
        }

        @Override
        public int insert(VehImportEventReplay replay) {
            if (replay.getId() == null) {
                replay.setId(seq++);
            }
            store.put(replay.getReplayId(), replay);
            return 1;
        }

        @Override
        public int update(VehImportEventReplay replay) {
            store.put(replay.getReplayId(), replay);
            return 1;
        }

        @Override
        public List<VehImportEventReplay> selectList(VehImportEventReplay replay) {
            return new ArrayList<>(store.values());
        }

        @Override
        public long countRunningByVehImportDataId(Long vehImportDataId) {
            return store.values().stream()
                    .filter(r -> vehImportDataId.equals(r.getVehImportDataId())
                            && "RUNNING".equals(r.getStatus()))
                    .count();
        }

        @Override
        public List<VehImportEventReplay> selectTimeoutRunningRecords(int timeoutMinutes) {
            return List.of();
        }

        @Override
        public int updateTimeoutRunningToFailed(int timeoutMinutes) {
            return 0;
        }
    }

    /** 内存 Fake 明细仓储 */
    private static class FakeItemRepository implements VehImportEventReplayItemRepository {
        final Map<String, VehImportEventReplayItem> store = new LinkedHashMap<>();
        long seq = 1L;

        @Override
        public VehImportEventReplayItem selectById(Long id) {
            return store.values().stream().filter(i -> id.equals(i.getId())).findFirst().orElse(null);
        }

        @Override
        public int insert(VehImportEventReplayItem item) {
            if (item.getId() == null) {
                item.setId(seq++);
            }
            store.put(key(item), item);
            return 1;
        }

        @Override
        public int update(VehImportEventReplayItem item) {
            store.put(key(item), item);
            return 1;
        }

        @Override
        public VehImportEventReplayItem selectByUniqueKey(String replayId, String actionType, String aggregateType,
                                                          String aggregateId, Long aggregateVersion) {
            return store.get(replayId + ":" + actionType + ":" + aggregateType + ":" + aggregateId + ":" + aggregateVersion);
        }

        @Override
        public List<VehImportEventReplayItem> selectListByReplayId(String replayId) {
            List<VehImportEventReplayItem> list = new ArrayList<>();
            store.values().forEach(i -> {
                if (replayId.equals(i.getReplayId())) {
                    list.add(i);
                }
            });
            return list;
        }

        private String key(VehImportEventReplayItem i) {
            return i.getReplayId() + ":" + i.getActionType() + ":" + i.getAggregateType()
                    + ":" + i.getAggregateId() + ":" + i.getAggregateVersion();
        }
    }

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = spy(new TransactionTemplate());
        lenient().doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        }).when(tx).execute(any(TransactionCallback.class));

        masterRepository = new FakeMasterRepository();
        itemRepository = new FakeItemRepository();
        registry = new VehicleImportReplayActionRegistry(List.of(
                fakeAction("PRODUCE_EVENT", 1, VehicleImportReplayActionResult.Outcome.QUEUED),
                fakeAction("TOL_LIFECYCLE_ENSURE", 1, VehicleImportReplayActionResult.Outcome.SUCCESS),
                fakeAction("EOL_LIFECYCLE_ENSURE", 1, VehicleImportReplayActionResult.Outcome.SKIPPED),
                fakeAction("BINDING_EVENT_REPLAY", 1, VehicleImportReplayActionResult.Outcome.QUEUED),
                fakeAction("SOFTWARE_INVENTORY_EVENT_REPLAY", 1, VehicleImportReplayActionResult.Outcome.FAILED_FINAL)));

        appService = new VehImportEventReplayAppService(
                vehImportDataRepository, masterRepository, itemRepository,
                new VehImportReplayExtractor(), registry, vmdOutboxRepository, tx);
    }

    /**
     * 构造 fake 动作：每个 VIN 返回 fixedTargetCount 个目标，execute 返回固定 outcome
     */
    private VehicleImportReplayAction fakeAction(String actionType, int fixedTargetCount,
                                                 VehicleImportReplayActionResult.Outcome outcome) {
        return new VehicleImportReplayAction() {
            @Override
            public String actionType() {
                return actionType;
            }

            @Override
            public List<VehicleImportReplayActionTarget> plan(VehicleImportReplayActionContext context) {
                List<VehicleImportReplayActionTarget> targets = new ArrayList<>();
                for (int i = 0; i < fixedTargetCount; i++) {
                    targets.add(new VehicleImportReplayActionTarget(
                            "VEHICLE", context.getVin(), i + 1L, null));
                }
                return targets;
            }

            @Override
            public VehicleImportReplayActionResult execute(VehicleImportReplayActionContext context,
                                                           VehicleImportReplayActionTarget target) {
                return switch (outcome) {
                    case QUEUED -> {
                        VmdOutbox outbox = VmdOutbox.builder()
                                .eventId(UUID.randomUUID().toString())
                                .eventType(actionType)
                                .aggregateType("VEHICLE")
                                .aggregateId(context.getVin())
                                .aggregateVersion(target.aggregateVersion())
                                .topic("vmd.test.topic")
                                .messageKey(context.getVin())
                                .payload("{}")
                                .publishState("PENDING")
                                .retryCount(0)
                                .sourceType("IMPORT_EVENT_REPLAY")
                                .sourceRefId(context.getReplayId())
                                .createTime(LocalDateTime.now())
                                .build();
                        yield VehicleImportReplayActionResult.queued(outbox.getEventId(), outbox);
                    }
                    case SUCCESS -> VehicleImportReplayActionResult.success();
                    case SKIPPED -> VehicleImportReplayActionResult.skipped("ALREADY_EXISTS");
                    default -> VehicleImportReplayActionResult.failedFinal("806081", "测试失败");
                };
            }
        };
    }

    private VehImportData buildImportData(Long id, String type, String batchNum, String data) {
        return VehImportData.builder()
                .id(id).batchNum(batchNum).type(type).version("1.0")
                .data(data).handle(Boolean.TRUE).createTime(LocalDateTime.now())
                .build();
    }

    private String tolData(String vin) {
        return "{\"REQUEST\":{\"DATA\":{\"ITEMS\":[{\"VIN\":\"" + vin + "\",\"PARTS\":["
                + "{\"ASSEMBLY_PART_NO\":\"PN001\",\"SN\":\"SN001\"}]}]}}}";
    }

    private String produceData(String vin) {
        return "{\"REQUEST\":{\"DATA\":{\"ITEMS\":[{\"VIN\":\"" + vin + "\"}]}}}";
    }

    @Test
    @DisplayName("原导入记录不存在 → NotAllowed")
    void replay_importDataMissing() {
        when(vehImportDataRepository.selectById(99L)).thenReturn(null);
        assertThrows(VehicleImportEventReplayNotAllowedException.class,
                () -> appService.replay(99L, ReplayVehicleImportEventCmd.builder().build(), "op1", "操作人"));
    }

    @Test
    @DisplayName("非PRODUCE/TOL/EOL类型 → NotAllowed")
    void replay_unsupportedType() {
        when(vehImportDataRepository.selectById(1L)).thenReturn(buildImportData(1L, "OTHER", "B001", "{}"));
        assertThrows(VehicleImportEventReplayNotAllowedException.class,
                () -> appService.replay(1L, ReplayVehicleImportEventCmd.builder().build(), "op1", "操作人"));
    }

    @Test
    @DisplayName("未处理成功 → NotAllowed")
    void replay_notHandled() {
        VehImportData data = buildImportData(1L, "TOL", "B001", tolData("VIN001"));
        data.setHandle(Boolean.FALSE);
        when(vehImportDataRepository.selectById(1L)).thenReturn(data);
        assertThrows(VehicleImportEventReplayNotAllowedException.class,
                () -> appService.replay(1L, ReplayVehicleImportEventCmd.builder().build(), "op1", "操作人"));
    }

    @Test
    @DisplayName("PRODUCE补发应仅执行PRODUCE_EVENT并保持QUEUED状态（CR-039兼容）")
    void replayProduceOnlyQueued() {
        when(vehImportDataRepository.selectById(1L)).thenReturn(buildImportData(1L, "PRODUCE", "B001", produceData("VIN001")));

        ReplayEventResult result = appService.replay(1L, ReplayVehicleImportEventCmd.builder().build(), "op1", "操作人");

        assertEquals("QUEUED", masterRepository.selectByReplayId(result.getReplayId()).getStatus());
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getQueuedCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(1, result.getActionResults().size());
        assertEquals("PRODUCE_EVENT", result.getActionResults().get(0).getActionType());
        // 逐项审计落账
        assertEquals(1, itemRepository.selectListByReplayId(result.getReplayId()).size());
        assertEquals("QUEUED", itemRepository.selectListByReplayId(result.getReplayId()).get(0).getStatus());
    }

    @Test
    @DisplayName("TOL补发应编排生命周期+绑定+软件实装，状态收敛与逐项审计")
    void replayTolOrchestration() {
        when(vehImportDataRepository.selectById(1L)).thenReturn(buildImportData(1L, "TOL", "B001", tolData("VIN001")));

        ReplayEventResult result = appService.replay(1L, ReplayVehicleImportEventCmd.builder().build(), "op1", "操作人");

        assertEquals(3, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());   // TOL_LIFECYCLE_ENSURE
        assertEquals(1, result.getQueuedCount());    // BINDING_EVENT_REPLAY
        assertEquals(1, result.getFailureCount());   // SOFTWARE_INVENTORY_EVENT_REPLAY
        assertEquals("PARTIAL_FAILED", masterRepository.selectByReplayId(result.getReplayId()).getStatus());
        assertEquals(3, itemRepository.selectListByReplayId(result.getReplayId()).size());
        // 主任务记录 importType 与 requestedActions
        VehImportEventReplay master = masterRepository.selectByReplayId(result.getReplayId());
        assertEquals("TOL", master.getImportType());
        assertTrue(master.getRequestedActions().contains("TOL_LIFECYCLE_ENSURE"));
    }

    @Test
    @DisplayName("EOL补发混合状态应正确收敛（SKIPPED+QUEUED+FAILED → PARTIAL_FAILED）")
    void replayEolMixedStatus() {
        when(vehImportDataRepository.selectById(1L)).thenReturn(buildImportData(1L, "EOL", "B001", tolData("VIN001")));

        ReplayEventResult result = appService.replay(1L, ReplayVehicleImportEventCmd.builder().build(), "op1", "操作人");

        // fake：EOL_LIFECYCLE_ENSURE 返回 SKIPPED，BINDING QUEUED，SOFTWARE FAILED_FINAL
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getSkipCount());
        assertEquals(1, result.getQueuedCount());
        assertEquals(1, result.getFailureCount());
        VehImportEventReplay master = masterRepository.selectByReplayId(result.getReplayId());
        assertNotNull(master);
        assertEquals("PARTIAL_FAILED", master.getStatus());
        assertTrue(result.getActionResults().stream()
                .anyMatch(a -> "EOL_LIFECYCLE_ENSURE".equals(a.getActionType()) && a.getSkipCount() == 1));
    }

    @Test
    @DisplayName("replayId已存在且终态应幂等返回既有结果，不重复执行")
    void replayIdempotentReplayId() {
        when(vehImportDataRepository.selectById(1L)).thenReturn(buildImportData(1L, "PRODUCE", "B001", produceData("VIN001")));

        ReplayEventResult first = appService.replay(1L,
                ReplayVehicleImportEventCmd.builder().requestId("REQ-001").build(), "op1", "操作人");
        int itemCountAfterFirst = itemRepository.selectListByReplayId("REQ-001").size();

        ReplayEventResult second = appService.replay(1L,
                ReplayVehicleImportEventCmd.builder().requestId("REQ-001").build(), "op1", "操作人");

        assertEquals(first.getReplayId(), second.getReplayId());
        assertEquals(itemCountAfterFirst, itemRepository.selectListByReplayId("REQ-001").size());
    }

    @Test
    @DisplayName("同导入记录存在RUNNING主任务 → InProgress")
    void replayRunningMutex() {
        when(vehImportDataRepository.selectById(1L)).thenReturn(buildImportData(1L, "PRODUCE", "B001", produceData("VIN001")));

        appService.replay(1L, ReplayVehicleImportEventCmd.builder().requestId("RUN-001").build(), "op1", "操作人");
        // 模拟另一个并发请求（无 requestId → 新 UUID），此时主任务仍 RUNNING 会被拦截
        // 手动将主任务置为 RUNNING 以模拟并发窗口
        masterRepository.selectByReplayId("RUN-001").setStatus("RUNNING");

        assertThrows(VehicleImportEventReplayInProgressException.class,
                () -> appService.replay(1L, ReplayVehicleImportEventCmd.builder().build(), "op1", "操作人"));
    }

    @Test
    @DisplayName("未知动作类型 → ActionNotFound(806081)")
    void replayUnknownActionType() {
        when(vehImportDataRepository.selectById(1L)).thenReturn(buildImportData(1L, "TOL", "B001", tolData("VIN001")));
        assertThrows(VehicleImportEventReplayActionNotFoundException.class,
                () -> appService.replay(1L,
                        ReplayVehicleImportEventCmd.builder().actionTypes(List.of("UNKNOWN_ACTION")).build(), "op1", "操作人"));
    }

    @Test
    @DisplayName("动作类型不属于该导入类型 → ActionNotFound")
    void replayActionTypeNotApplicable() {
        when(vehImportDataRepository.selectById(1L)).thenReturn(buildImportData(1L, "PRODUCE", "B001", produceData("VIN001")));
        assertThrows(VehicleImportEventReplayActionNotFoundException.class,
                () -> appService.replay(1L,
                        ReplayVehicleImportEventCmd.builder().actionTypes(List.of("BINDING_EVENT_REPLAY")).build(), "op1", "操作人"));
    }

    @Test
    @DisplayName("preview应返回候选车辆数与分动作统计")
    void previewReturnsActions() {
        when(vehImportDataRepository.selectById(1L)).thenReturn(buildImportData(1L, "TOL", "B001", tolData("VIN001")));

        VehicleImportReplayPreview preview = appService.preview(1L);

        assertEquals("TOL", preview.getImportType());
        assertEquals("B001", preview.getBatchNum());
        assertEquals(1, preview.getCandidateVinCount());
        assertEquals(3, preview.getActions().size());
        assertEquals("TOL_LIFECYCLE_ENSURE", preview.getActions().get(0).getActionType());
    }

    @Test
    @DisplayName("preview对不允许补发的记录应抛NotAllowed")
    void previewNotAllowed() {
        VehImportData data = buildImportData(1L, "TOL", "B001", tolData("VIN001"));
        data.setHandle(Boolean.FALSE);
        when(vehImportDataRepository.selectById(1L)).thenReturn(data);
        assertThrows(VehicleImportEventReplayNotAllowedException.class, () -> appService.preview(1L));
    }
}
