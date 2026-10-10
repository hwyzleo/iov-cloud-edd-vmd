package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ReplayPartImportPostProcessCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ReplayPostProcessResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.postprocess.PartPostProcessActionContext;
import net.hwyz.iov.cloud.edd.vmd.service.application.postprocess.PartPostProcessActionHandler;
import net.hwyz.iov.cloud.edd.vmd.service.application.postprocess.PartPostProcessActionResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.postprocess.PartPostProcessActionRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.PartImportPostProcessReplayExtractor;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartImportPostProcessReplayInProgressException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartImportPostProcessReplayNotAllowedException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportPostProcessReplay;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportPostProcessReplayItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartPostProcessActionStatus;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartPostProcessActionType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartPostProcessReplayStatus;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportPostProcessReplayItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportPostProcessReplayRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 零件导入后置处理重放编排应用服务单元测试
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 * <p>
 * 覆盖：资格校验、RUNNING 互斥、动作级幂等（RESUME）、候选不存在 FAILED_FINAL、
 * 状态聚合、retryFailedOnly 播种，以及不修改 part_import_data / part_info / vehicle_part 主体。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PartImportPostProcessReplayAppService 测试")
class PartImportPostProcessReplayAppServiceTest {

    @Mock
    private PartImportDataRepository partImportDataRepository;
    @Mock
    private PartImportPostProcessReplayRepository replayRepository;
    @Mock
    private PartImportPostProcessReplayItemRepository itemRepository;
    @Mock
    private PartPostProcessActionRegistry actionRegistry;
    @Mock
    private PartInfoRepository partInfoRepository;
    @Mock
    private VehiclePartRepository vehiclePartRepository;
    @Mock
    private MdmPartRepository mdmPartRepository;
    @Mock
    private MdmVehicleNodeRepository mdmVehicleNodeRepository;
    @Mock
    private VmdOutboxRepository vmdOutboxRepository;
    @Mock
    private PartPostProcessActionHandler inboundHandler;
    @Mock
    private PartPostProcessActionHandler skippedHandler;

    private PartImportPostProcessReplayAppService appService;

    /** 内存 Fake 明细仓储：insert/update 落内存，selectList 可读回，供状态聚合断言 */
    private static class FakeItemRepository implements PartImportPostProcessReplayItemRepository {
        final Map<String, PartImportPostProcessReplayItem> store = new LinkedHashMap<>();
        long seq = 1L;

        @Override
        public PartImportPostProcessReplayItem selectById(Long id) {
            return store.values().stream().filter(i -> id.equals(i.getId())).findFirst().orElse(null);
        }

        @Override
        public int insert(PartImportPostProcessReplayItem item) {
            if (item.getId() == null) {
                item.setId(seq++);
            }
            store.put(key(item), item);
            return 1;
        }

        @Override
        public int update(PartImportPostProcessReplayItem item) {
            store.put(key(item), item);
            return 1;
        }

        @Override
        public PartImportPostProcessReplayItem selectByUniqueKey(String replayId, String partCode, String sn, String actionType) {
            return store.get(replayId + ":" + partCode + ":" + sn + ":" + actionType);
        }

        @Override
        public List<PartImportPostProcessReplayItem> selectListByReplayId(String replayId) {
            List<PartImportPostProcessReplayItem> list = new ArrayList<>();
            store.values().forEach(i -> {
                if (replayId.equals(i.getReplayId())) {
                    list.add(i);
                }
            });
            return list;
        }

        @Override
        public List<PartImportPostProcessReplayItem> selectRetryableByReplayId(String replayId) {
            List<PartImportPostProcessReplayItem> list = new ArrayList<>();
            store.values().forEach(i -> {
                if (replayId.equals(i.getReplayId())
                        && PartPostProcessActionStatus.valOf(i.getStatus()).retryable()) {
                    list.add(i);
                }
            });
            return list;
        }

        private String key(PartImportPostProcessReplayItem i) {
            return i.getReplayId() + ":" + i.getPartCode() + ":" + i.getSn() + ":" + i.getActionType();
        }
    }

    private final FakeItemRepository fakeItemRepository = new FakeItemRepository();

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = spy(new TransactionTemplate());
        lenient().doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        }).when(tx).execute(any(TransactionCallback.class));

        appService = new PartImportPostProcessReplayAppService(
                partImportDataRepository, replayRepository, fakeItemRepository,
                new PartImportPostProcessReplayExtractor(), actionRegistry,
                partInfoRepository, vehiclePartRepository, mdmPartRepository, mdmVehicleNodeRepository,
                vmdOutboxRepository, tx);

        lenient().when(actionRegistry.getHandler(PartPostProcessActionType.PART_INBOUND_EVENT.getValue()))
                .thenReturn(inboundHandler);
        lenient().when(actionRegistry.getHandler(anyString())).thenAnswer(invocation -> {
            String actionType = invocation.getArgument(0);
            return PartPostProcessActionType.PART_INBOUND_EVENT.getValue().equals(actionType)
                    ? inboundHandler : skippedHandler;
        });
        lenient().when(inboundHandler.execute(any())).thenAnswer(invocation -> {
            PartPostProcessActionContext ctx = invocation.getArgument(0);
            VmdOutbox outbox = VmdOutbox.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("PartInboundEvent")
                    .aggregateType("PART_INSTANCE")
                    .aggregateId(ctx.getPartCode() + ":" + ctx.getSn())
                    .topic("vmd.part-inbound.changed")
                    .messageKey(ctx.getPartCode() + ":" + ctx.getSn())
                    .payload("{}")
                    .publishState("PENDING")
                    .retryCount(0)
                    .sourceType("PART_POST_PROCESS_REPLAY")
                    .sourceRefId(ctx.getReplayId())
                    .createTime(LocalDateTime.now())
                    .build();
            return PartPostProcessActionResult.queued(outbox.getEventId(), outbox);
        });
        lenient().when(skippedHandler.execute(any()))
                .thenReturn(PartPostProcessActionResult.skipped("NOT_APPLICABLE"));
    }

    private PartImportData buildImportData(Long id) {
        return PartImportData.builder().id(id).batchNum("B001").partCode("PN001")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP001\"},\"DATA\":{\"ITEMS\":"
                        + "[{\"SN\":\"SN001\",\"ASSEMBLY_PART_NO\":\"PN001\"}]}}}")
                .handle(Boolean.TRUE).createTime(LocalDateTime.now()).build();
    }

    private ReplayPartImportPostProcessCmd defaultRequest() {
        return ReplayPartImportPostProcessCmd.builder().reason("测试重放").build();
    }

    @Test
    @DisplayName("原导入记录不存在 → NotAllowed(806078)")
    void replay_importDataMissing() {
        when(partImportDataRepository.selectById(99L)).thenReturn(null);
        assertThrows(PartImportPostProcessReplayNotAllowedException.class,
                () -> appService.replay(99L, defaultRequest(), "op1", "操作人"));
    }

    @Test
    @DisplayName("存在执行中的完整重放 → InProgress(806079)")
    void replay_runningConflict() {
        PartImportData importData = buildImportData(1L);
        when(partImportDataRepository.selectById(1L)).thenReturn(importData);
        when(replayRepository.countRunningByPartImportDataId(1L)).thenReturn(1L);
        assertThrows(PartImportPostProcessReplayInProgressException.class,
                () -> appService.replay(1L, defaultRequest(), "op1", "操作人"));
    }

    @Test
    @DisplayName("全量重放：事件动作入 Outbox(QUEUED)、其余 SKIPPED，主任务 SUCCESS，主体零修改")
    void replay_fullMode_aggregatesStatus() {
        PartImportData importData = buildImportData(1L);
        when(partImportDataRepository.selectById(1L)).thenReturn(importData);
        when(replayRepository.countRunningByPartImportDataId(anyLong())).thenReturn(0L);
        PartInfo partInfo = PartInfo.builder().id(1L).partCode("PN001").sn("SN001")
                .vehicleNodeCode("TBOX_5G").build();
        when(partInfoRepository.selectByPartCodeAndSn("PN001", "SN001")).thenReturn(partInfo);
        when(vehiclePartRepository.selectActiveByPartId(1L)).thenReturn(null);
        when(mdmPartRepository.selectByCode("PN001")).thenReturn(null);
        when(mdmVehicleNodeRepository.selectByCode(anyString())).thenReturn(null);

        ReplayPostProcessResult result = appService.replay(1L, defaultRequest(), "op1", "操作人");

        assertNotNull(result.getReplayId());
        assertEquals(1, result.getItemCount());
        assertEquals(6, result.getActionCount());
        assertEquals(1, result.getQueuedCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(5, result.getSkippedCount());
        assertEquals(0, result.getFailureCount());

        // 动作明细断言
        List<PartImportPostProcessReplayItem> items = fakeItemRepository.selectListByReplayId(result.getReplayId());
        assertEquals(6, items.size());
        long queued = items.stream().filter(i -> "QUEUED".equals(i.getStatus())).count();
        long skipped = items.stream().filter(i -> "SKIPPED".equals(i.getStatus())).count();
        assertEquals(1, queued);
        assertEquals(5, skipped);

        // 主体零修改：不写 part_import_data / part_info / vehicle_part
        verify(partImportDataRepository, never()).update(any());
        verify(partInfoRepository, never()).update(any());
        verify(vehiclePartRepository, never()).update(any());
        // 事件 Outbox 写入 1 条
        verify(vmdOutboxRepository, org.mockito.Mockito.times(1)).insert(any(VmdOutbox.class));
    }

    @Test
    @DisplayName("候选实例当前不存在 → 各动作 FAILED_FINAL(PART_NOT_FOUND)，无 Outbox 写入")
    void replay_partNotFound() {
        PartImportData importData = buildImportData(1L);
        when(partImportDataRepository.selectById(1L)).thenReturn(importData);
        when(replayRepository.countRunningByPartImportDataId(anyLong())).thenReturn(0L);
        when(partInfoRepository.selectByPartCodeAndSn("PN001", "SN001")).thenReturn(null);
        when(mdmPartRepository.selectByCode("PN001")).thenReturn(null);

        ReplayPostProcessResult result = appService.replay(1L, defaultRequest(), "op1", "操作人");

        assertEquals(6, result.getFailureCount());
        assertEquals(6, result.getActionCount());
        List<PartImportPostProcessReplayItem> items = fakeItemRepository.selectListByReplayId(result.getReplayId());
        assertEquals(6, items.size());
        assertTrue(items.stream().allMatch(i -> "FAILED_FINAL".equals(i.getStatus())
                && "PART_NOT_FOUND".equals(i.getErrorCode())));
        verify(vmdOutboxRepository, never()).insert(any(VmdOutbox.class));
    }

    @Test
    @DisplayName("retryFailedOnly 无历史重放任务 → NotAllowed")
    void replay_retryFailedOnlyWithoutHistory() {
        PartImportData importData = buildImportData(1L);
        when(partImportDataRepository.selectById(1L)).thenReturn(importData);
        when(replayRepository.selectLatestTerminalByPartImportDataId(1L)).thenReturn(null);
        ReplayPartImportPostProcessCmd request = ReplayPartImportPostProcessCmd.builder()
                .retryFailedOnly(Boolean.TRUE).build();
        assertThrows(PartImportPostProcessReplayNotAllowedException.class,
                () -> appService.replay(1L, request, "op1", "操作人"));
    }

    @Test
    @DisplayName("RESUME：复用 replayId 仅继续失败/未完成动作，成功项不重跑")
    void replay_resumeOnlyFailedItems() {
        // 已有重放任务：PART_INBOUND_EVENT 成功、SECURITY_PRESET 失败
        String replayId = "replay-existing";
        PartImportPostProcessReplay existingMaster = PartImportPostProcessReplay.builder()
                .id(1L).replayId(replayId).partImportDataId(1L).batchNum("B001")
                .status(PartPostProcessReplayStatus.PARTIAL_SUCCESS.getValue())
                .build();
        fakeItemRepository.insert(PartImportPostProcessReplayItem.builder()
                .replayId(replayId).partCode("PN001").sn("SN001")
                .actionType(PartPostProcessActionType.PART_INBOUND_EVENT.getValue())
                .status(PartPostProcessActionStatus.SUCCESS.getValue()).attemptCount(1).build());
        fakeItemRepository.insert(PartImportPostProcessReplayItem.builder()
                .replayId(replayId).partCode("PN001").sn("SN001")
                .actionType(PartPostProcessActionType.SECURITY_PRESET.getValue())
                .status(PartPostProcessActionStatus.FAILED_RETRYABLE.getValue()).attemptCount(1)
                .errorMessage("安全常量预置失败: KMS不可用").build());

        PartImportData importData = buildImportData(1L);
        when(partImportDataRepository.selectById(1L)).thenReturn(importData);
        when(replayRepository.selectByReplayId(replayId)).thenReturn(existingMaster);
        when(replayRepository.countRunningByPartImportDataId(1L)).thenReturn(0L);
        PartInfo partInfo = PartInfo.builder().id(1L).partCode("PN001").sn("SN001")
                .vehicleNodeCode("TBOX_5G").build();
        when(partInfoRepository.selectByPartCodeAndSn("PN001", "SN001")).thenReturn(partInfo);
        when(vehiclePartRepository.selectActiveByPartId(1L)).thenReturn(null);
        when(mdmPartRepository.selectByCode("PN001")).thenReturn(null);
        when(mdmVehicleNodeRepository.selectByCode(anyString())).thenReturn(null);
        // SECURITY_PRESET handler 执行成功
        when(skippedHandler.execute(any())).thenReturn(PartPostProcessActionResult.success());

        ReplayPartImportPostProcessCmd request = ReplayPartImportPostProcessCmd.builder()
                .requestId(replayId).retryFailedOnly(Boolean.TRUE).build();
        ReplayPostProcessResult result = appService.replay(1L, request, "op1", "操作人");

        // 只继续 SECURITY_PRESET（1 个动作）
        assertEquals(1, result.getActionCount());
        // 主任务计数为该 replayId 累计：历史 PART_INBOUND_EVENT(SUCCESS) + 本次 SECURITY_PRESET(SUCCESS)
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getQueuedCount());
        assertEquals(0, result.getFailureCount());
        // 成功的 PART_INBOUND_EVENT 未被重跑：inboundHandler.execute 未被调用
        verify(inboundHandler, never()).execute(any());
        // SECURITY_PRESET 失败项已改为 SUCCESS
        PartImportPostProcessReplayItem retried = fakeItemRepository.selectByUniqueKey(
                replayId, "PN001", "SN001", PartPostProcessActionType.SECURITY_PRESET.getValue());
        assertNotNull(retried);
        assertEquals(PartPostProcessActionStatus.SUCCESS.getValue(), retried.getStatus());
        assertEquals(2, retried.getAttemptCount());
    }

    @Test
    @DisplayName("未知动作范围 → ActionNotFound(806080)")
    void replay_unknownActionScope() {
        PartImportData importData = buildImportData(1L);
        when(partImportDataRepository.selectById(1L)).thenReturn(importData);
        ReplayPartImportPostProcessCmd request = ReplayPartImportPostProcessCmd.builder()
                .scope(List.of("UNKNOWN_ACTION")).build();
        assertThrows(net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartImportPostProcessReplayActionNotFoundException.class,
                () -> appService.replay(1L, request, "op1", "操作人"));
    }

    @Test
    @DisplayName("GET /replays/{replayId}：不存在 → NotAllowed")
    void getReplay_notFound() {
        when(replayRepository.selectByReplayId("nope")).thenReturn(null);
        assertThrows(PartImportPostProcessReplayNotAllowedException.class,
                () -> appService.getReplayByReplayId("nope"));
    }
}
