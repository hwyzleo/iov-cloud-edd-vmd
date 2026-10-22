package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.OperatorContext;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.TerminateStaleProvisioningCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ProvisioningBlockingDiagnosisResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ProvisioningTerminationResult;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyProvisioningTerminateDirectoryConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyProvisioningTerminateDisabledException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyProvisioningTerminateNotAllowedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyProvisioningTerminateReconcileFirstException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyProvisioningTerminateStaleViewException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyProvisioningTerminateSuccessFactExistsException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioningItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ProvisioningDeviceResult;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.TerminationReasonCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceBusinessKeyRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.BusinessKeyProvisioningMaintenanceMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BusinessKeyProvisioningMaintenanceAppService 单元测试（VMD-DSN-CR-065 §9.1/§9.2）。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BusinessKeyProvisioningMaintenanceAppServiceTest {

    private static final String REQUEST_ID = "V2C-REQ-CR065";
    private static final String DEVICE_SN = "CR065-TBOX-0001";
    private static final String DOMAIN = "V2C_COMM";

    @Mock
    private BusinessKeyProvisioningRepository provisioningRepository;
    @Mock
    private BusinessKeyProvisioningItemRepository provisioningItemRepository;
    @Mock
    private DeviceBusinessKeyRepository deviceBusinessKeyRepository;
    @Mock
    private BoundDeviceIdentityResolver boundDeviceIdentityResolver;
    @Mock
    private ActiveDeviceCertificateResolver activeDeviceCertificateResolver;
    @Mock
    private VmdOutboxRepository vmdOutboxRepository;
    @Mock
    private ProvisioningOperationEvidence operationEvidence;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;

    private BusinessKeyProvisioningMaintenanceAppService service;
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final BusinessKeyProvisioningMaintenanceMetrics metrics =
            new BusinessKeyProvisioningMaintenanceMetrics(meterRegistry);

    private final Map<Long, DeviceBusinessKey> keyStore = new HashMap<>();
    private final Map<Long, BusinessKeyProvisioningItem> itemStore = new HashMap<>();
    private final AtomicLong keyIdSeq = new AtomicLong(1);
    private final AtomicLong itemIdSeq = new AtomicLong(1);
    private BusinessKeyProvisioning batchRef;

    @BeforeEach
    void setUp() {
        service = new BusinessKeyProvisioningMaintenanceAppService(
                provisioningRepository, provisioningItemRepository, deviceBusinessKeyRepository,
                new BusinessKeyProvisioningDomainService(),
                boundDeviceIdentityResolver, activeDeviceCertificateResolver,
                vmdOutboxRepository, operationEvidence, metrics, transactionManager);
        ReflectionTestUtils.setField(service, "terminateEnabled", true);
        ReflectionTestUtils.setField(service, "staleThresholdSeconds", 0L);

        lenient().when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    }

    private BusinessKeyProvisioning newBatch(BusinessKeyProvisioningState state, LocalDateTime modifyTime) {
        return BusinessKeyProvisioning.builder()
                .id(1L)
                .requestId(REQUEST_ID)
                .deviceSn(DEVICE_SN)
                .businessDomain(DOMAIN)
                .status(state)
                .startedAt(LocalDateTime.now().minusDays(3))
                .modifyTime(modifyTime)
                .createTime(LocalDateTime.now().minusDays(3))
                .build();
    }

    private DeviceBusinessKey newPendingKey(String purpose, Long version, Long id) {
        return DeviceBusinessKey.builder()
                .id(id)
                .deviceSn(DEVICE_SN)
                .businessDomain(DOMAIN)
                .purpose(purpose)
                .businessKeyVersion(version)
                .keyState(BusinessKeyState.PENDING)
                .keyId("key-" + purpose)
                .requestId(REQUEST_ID)
                .build();
    }

    private BusinessKeyProvisioningItem newItem(Long id, Long directoryKeyId, String purpose, String operationId) {
        return BusinessKeyProvisioningItem.builder()
                .id(id)
                .provisioningId(1L)
                .purpose(purpose)
                .keyId("key-" + purpose)
                .businessKeyVersion(1L)
                .directoryKeyId(directoryKeyId)
                .operationId(operationId)
                .deviceResult(ProvisioningDeviceResult.PENDING)
                .build();
    }

    /**
     * 装配“滞留 PREPARING + 两个 PENDING 目录”的标准前置（无 operation，无成功事实）。
     */
    private void setUpStuckPreparing(List<BusinessKeyProvisioningItem> items, List<DeviceBusinessKey> directories) {
        batchRef = newBatch(BusinessKeyProvisioningState.PREPARING, LocalDateTime.now().minusHours(2));
        when(provisioningRepository.selectByRequestId(REQUEST_ID)).thenReturn(batchRef);
        when(provisioningRepository.selectByRequestIdForUpdate(REQUEST_ID)).thenReturn(batchRef);
        when(provisioningItemRepository.selectByProvisioningId(1L)).thenReturn(items);
        when(provisioningItemRepository.selectByProvisioningIdForUpdate(1L)).thenReturn(items);
        directories.forEach(d -> when(deviceBusinessKeyRepository.selectByIdForUpdate(d.getId())).thenReturn(d));
        when(deviceBusinessKeyRepository.markPendingKeysFailed(anyString(), anyList(), anyString()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    List<String> keyIds = inv.getArgument(1);
                    int n = 0;
                    for (DeviceBusinessKey d : directories) {
                        if (d.getKeyState() == BusinessKeyState.PENDING && keyIds.contains(d.getKeyId())) {
                            d.markFailed("ops_termination");
                            n++;
                        }
                    }
                    return n;
                });
        when(provisioningRepository.update(any(BusinessKeyProvisioning.class))).thenReturn(1);
        // operation 证据：无 operationId → NOT_STARTED
        Map<Long, ProvisioningOperationEvidence.OperationOutcome> outcomes = new HashMap<>();
        items.forEach(i -> outcomes.put(i.getId(), ProvisioningOperationEvidence.OperationOutcome.NOT_STARTED));
        when(operationEvidence.query(anyList())).thenReturn(outcomes);
        // 成功 Outbox 反查为空
        when(vmdOutboxRepository.countByAggregateAndEventType(anyString(), anyString())).thenReturn(0L);
    }

    private TerminateStaleProvisioningCommand command() {
        return TerminateStaleProvisioningCommand.builder()
                .maintenanceRequestId("op-0001")
                .requestId(REQUEST_ID)
                .expectedState(BusinessKeyProvisioningState.PREPARING)
                .expectedModifyTime(batchRef != null ? batchRef.getModifyTime() : null)
                .reasonCode(TerminationReasonCode.CERTIFICATE_CHANGED_DIGEST_CONFLICT)
                .reason("证书已升级 F2，清理旧批次")
                .ticketNo("T-20261022-001")
                .build();
    }

    private OperatorContext operator() {
        return OperatorContext.builder().operatorId("1001").operatorName("ops-user").build();
    }

    // ================= §9.1 单元测试矩阵 =================

    @Test
    @DisplayName("PREPARING 超阈值、无 operation、无副作用 → 可终止，批次与目录按规则失败化")
    void eligible_preparingNoOperation_terminates() {
        List<DeviceBusinessKey> directories = List.of(
                newPendingKey("UPLINK_AEAD", 1L, 1L), newPendingKey("DOWNLINK_AEAD", 2L, 2L));
        List<BusinessKeyProvisioningItem> items = List.of(
                newItem(1L, 1L, "UPLINK_AEAD", null), newItem(2L, 2L, "DOWNLINK_AEAD", null));
        setUpStuckPreparing(items, directories);

        ProvisioningTerminationResult result = service.terminateStale(command(), operator());

        assertTrue(result.isChanged());
        assertTrue(result.isBlockingCleared());
        assertTrue(result.isReopenAllowed());
        assertEquals("PREPARING", result.getBeforeState());
        assertEquals("FAILED", result.getAfterState());
        assertEquals(2, result.getFailedPendingKeyCount());
        verify(provisioningRepository).update(batchRef);
        assertEquals(BusinessKeyProvisioningState.FAILED, batchRef.getStatus());
        // 目录均已 FAILED
        assertTrue(directories.stream().allMatch(d -> d.getKeyState() == BusinessKeyState.FAILED));
        // fail_reason 为紧凑非敏感终止码 + 审计关联号，不含操作者
        assertNotNull(batchRef.getFailReason());
        assertTrue(batchRef.getFailReason().startsWith("CERTIFICATE_CHANGED_DIGEST_CONFLICT"));
        assertFalse(batchRef.getFailReason().contains("ops-user"));
    }

    @Test
    @DisplayName("operation 明确 FAILED → 可终止")
    void operationConfirmedFailed_terminates() {
        List<DeviceBusinessKey> directories = List.of(newPendingKey("UPLINK_AEAD", 1L, 1L));
        List<BusinessKeyProvisioningItem> items = List.of(newItem(1L, 1L, "UPLINK_AEAD", "op-failed"));
        setUpStuckPreparing(items, directories);
        when(operationEvidence.query(anyList())).thenReturn(Map.of(
                1L, ProvisioningOperationEvidence.OperationOutcome.CONFIRMED_FAILED));

        ProvisioningTerminationResult result = service.terminateStale(command(), operator());
        assertTrue(result.isChanged());
        assertEquals("FAILED", result.getAfterState());
    }

    @Test
    @DisplayName("operation SUCCEEDED → 拒绝终止，不得伪造成功")
    void operationSucceeded_rejected() {
        List<DeviceBusinessKey> directories = List.of(newPendingKey("UPLINK_AEAD", 1L, 1L));
        List<BusinessKeyProvisioningItem> items = List.of(newItem(1L, 1L, "UPLINK_AEAD", "op-succeeded"));
        setUpStuckPreparing(items, directories);
        when(operationEvidence.query(anyList())).thenReturn(Map.of(
                1L, ProvisioningOperationEvidence.OperationOutcome.SUCCEEDED));

        assertThrows(BusinessKeyProvisioningTerminateSuccessFactExistsException.class,
                () -> service.terminateStale(command(), operator()));
        verify(provisioningRepository, never()).update(any(BusinessKeyProvisioning.class));
    }

    @Test
    @DisplayName("operation PROCESSING/UNKNOWN → RECONCILE_FIRST，禁止终止")
    void operationUnknown_reconcileFirst() {
        List<DeviceBusinessKey> directories = List.of(newPendingKey("UPLINK_AEAD", 1L, 1L));
        List<BusinessKeyProvisioningItem> items = List.of(newItem(1L, 1L, "UPLINK_AEAD", "op-unknown"));
        setUpStuckPreparing(items, directories);
        when(operationEvidence.query(anyList())).thenReturn(Map.of(
                1L, ProvisioningOperationEvidence.OperationOutcome.IN_PROGRESS_OR_UNKNOWN));

        assertThrows(BusinessKeyProvisioningTerminateReconcileFirstException.class,
                () -> service.terminateStale(command(), operator()));
        verify(provisioningRepository, never()).update(any(BusinessKeyProvisioning.class));
    }

    @Test
    @DisplayName("未超过最小滞留阈值 → NOT_STALE 拒绝")
    void notStale_rejected() {
        List<DeviceBusinessKey> directories = List.of(newPendingKey("UPLINK_AEAD", 1L, 1L));
        List<BusinessKeyProvisioningItem> items = List.of(newItem(1L, 1L, "UPLINK_AEAD", null));
        setUpStuckPreparing(items, directories);
        ReflectionTestUtils.setField(service, "staleThresholdSeconds", 86400L);

        assertThrows(BusinessKeyProvisioningTerminateNotAllowedException.class,
                () -> service.terminateStale(command(), operator()));
    }

    @Test
    @DisplayName("ACK 摘要已存在 → SUCCESS_FACT_EXISTS")
    void ackDigestPresent_successFact() {
        List<DeviceBusinessKey> directories = List.of(newPendingKey("UPLINK_AEAD", 1L, 1L));
        List<BusinessKeyProvisioningItem> items = List.of(newItem(1L, 1L, "UPLINK_AEAD", null));
        setUpStuckPreparing(items, directories);
        batchRef.setAckDigest("ack-digest-abc");

        assertThrows(BusinessKeyProvisioningTerminateSuccessFactExistsException.class,
                () -> service.terminateStale(command(), operator()));
    }

    @Test
    @DisplayName("item 存在 SUCCEEDED 结果 → SUCCESS_FACT_EXISTS")
    void itemSucceeded_successFact() {
        List<DeviceBusinessKey> directories = List.of(newPendingKey("UPLINK_AEAD", 1L, 1L));
        BusinessKeyProvisioningItem item = newItem(1L, 1L, "UPLINK_AEAD", null);
        item.markSucceeded("proof", LocalDateTime.now());
        List<BusinessKeyProvisioningItem> items = List.of(item);
        setUpStuckPreparing(items, directories);

        assertThrows(BusinessKeyProvisioningTerminateSuccessFactExistsException.class,
                () -> service.terminateStale(command(), operator()));
    }

    @Test
    @DisplayName("成功 Outbox 已存在 → SUCCESS_FACT_EXISTS")
    void successOutboxExists_successFact() {
        List<DeviceBusinessKey> directories = List.of(newPendingKey("UPLINK_AEAD", 1L, 1L));
        List<BusinessKeyProvisioningItem> items = List.of(newItem(1L, 1L, "UPLINK_AEAD", null));
        setUpStuckPreparing(items, directories);
        when(vmdOutboxRepository.countByAggregateAndEventType(anyString(), anyString())).thenReturn(1L);

        assertThrows(BusinessKeyProvisioningTerminateSuccessFactExistsException.class,
                () -> service.terminateStale(command(), operator()));
    }

    @Test
    @DisplayName("任一目录非 PENDING → DIRECTORY_CONFLICT")
    void directoryNotPending_conflict() {
        DeviceBusinessKey active = newPendingKey("UPLINK_AEAD", 1L, 1L);
        active.activate();
        List<DeviceBusinessKey> directories = List.of(active);
        List<BusinessKeyProvisioningItem> items = List.of(newItem(1L, 1L, "UPLINK_AEAD", null));
        setUpStuckPreparing(items, directories);

        assertThrows(BusinessKeyProvisioningTerminateDirectoryConflictException.class,
                () -> service.terminateStale(command(), operator()));
    }

    @Test
    @DisplayName("目录 requestId 不属于本批次 → DIRECTORY_CONFLICT")
    void directoryNotBelongingToBatch_conflict() {
        DeviceBusinessKey foreign = newPendingKey("UPLINK_AEAD", 1L, 1L);
        foreign.setRequestId("OTHER-REQ");
        List<DeviceBusinessKey> directories = List.of(foreign);
        List<BusinessKeyProvisioningItem> items = List.of(newItem(1L, 1L, "UPLINK_AEAD", null));
        setUpStuckPreparing(items, directories);

        assertThrows(BusinessKeyProvisioningTerminateDirectoryConflictException.class,
                () -> service.terminateStale(command(), operator()));
    }

    @Test
    @DisplayName("PENDING 失败化数量与预期不一致 → 整体回滚")
    void pendingCountMismatch_rollback() {
        List<DeviceBusinessKey> directories = List.of(
                newPendingKey("UPLINK_AEAD", 1L, 1L), newPendingKey("DOWNLINK_AEAD", 2L, 2L));
        List<BusinessKeyProvisioningItem> items = List.of(
                newItem(1L, 1L, "UPLINK_AEAD", null), newItem(2L, 2L, "DOWNLINK_AEAD", null));
        setUpStuckPreparing(items, directories);
        // 只失败 1 行，与预期 2 不符 → 回滚
        when(deviceBusinessKeyRepository.markPendingKeysFailed(anyString(), anyList(), anyString())).thenReturn(1);

        assertThrows(BusinessKeyProvisioningTerminateDirectoryConflictException.class,
                () -> service.terminateStale(command(), operator()));
        verify(provisioningRepository, never()).update(any(BusinessKeyProvisioning.class));
    }

    @Test
    @DisplayName("FAILED/EXPIRED 重复执行 → 幂等成功，changed=false 无状态变更")
    void terminal_idempotent() {
        batchRef = newBatch(BusinessKeyProvisioningState.FAILED, LocalDateTime.now().minusHours(2));
        when(provisioningRepository.selectByRequestId(REQUEST_ID)).thenReturn(batchRef);
        when(provisioningRepository.selectByRequestIdForUpdate(REQUEST_ID)).thenReturn(batchRef);

        ProvisioningTerminationResult result = service.terminateStale(command(), operator());

        assertFalse(result.isChanged());
        assertTrue(result.isBlockingCleared());
        assertTrue(result.isReopenAllowed());
        assertEquals("FAILED", result.getAfterState());
        verify(provisioningRepository, never()).update(any(BusinessKeyProvisioning.class));
    }

    @Test
    @DisplayName("COMPLETED 执行 → 拒绝")
    void completed_rejected() {
        batchRef = newBatch(BusinessKeyProvisioningState.COMPLETED, LocalDateTime.now().minusHours(2));
        when(provisioningRepository.selectByRequestId(REQUEST_ID)).thenReturn(batchRef);
        when(provisioningRepository.selectByRequestIdForUpdate(REQUEST_ID)).thenReturn(batchRef);
        TerminateStaleProvisioningCommand cmd = command();
        cmd.setExpectedState(BusinessKeyProvisioningState.COMPLETED);

        assertThrows(BusinessKeyProvisioningTerminateNotAllowedException.class,
                () -> service.terminateStale(cmd, operator()));
    }

    @Test
    @DisplayName("WAIT_DEVICE_ACK → 拒绝，仅由 ACK 超时任务治理")
    void waitDeviceAck_rejected() {
        batchRef = newBatch(BusinessKeyProvisioningState.WAIT_DEVICE_ACK, LocalDateTime.now().minusHours(2));
        when(provisioningRepository.selectByRequestId(REQUEST_ID)).thenReturn(batchRef);
        when(provisioningRepository.selectByRequestIdForUpdate(REQUEST_ID)).thenReturn(batchRef);
        TerminateStaleProvisioningCommand cmd = command();
        cmd.setExpectedState(BusinessKeyProvisioningState.WAIT_DEVICE_ACK);

        assertThrows(BusinessKeyProvisioningTerminateNotAllowedException.class,
                () -> service.terminateStale(cmd, operator()));
    }

    @Test
    @DisplayName("expectedState 与最新状态不一致 → STALE_VIEW")
    void staleView_stateMismatch() {
        List<DeviceBusinessKey> directories = List.of(newPendingKey("UPLINK_AEAD", 1L, 1L));
        List<BusinessKeyProvisioningItem> items = List.of(newItem(1L, 1L, "UPLINK_AEAD", null));
        setUpStuckPreparing(items, directories);
        TerminateStaleProvisioningCommand cmd = command();
        cmd.setExpectedState(BusinessKeyProvisioningState.RECONCILE_REQUIRED);

        assertThrows(BusinessKeyProvisioningTerminateStaleViewException.class,
                () -> service.terminateStale(cmd, operator()));
    }

    @Test
    @DisplayName("expectedModifyTime 与最新不一致 → STALE_VIEW")
    void staleView_modifyTimeMismatch() {
        List<DeviceBusinessKey> directories = List.of(newPendingKey("UPLINK_AEAD", 1L, 1L));
        List<BusinessKeyProvisioningItem> items = List.of(newItem(1L, 1L, "UPLINK_AEAD", null));
        setUpStuckPreparing(items, directories);
        TerminateStaleProvisioningCommand cmd = command();
        cmd.setExpectedModifyTime(LocalDateTime.now().minusDays(1));

        assertThrows(BusinessKeyProvisioningTerminateStaleViewException.class,
                () -> service.terminateStale(cmd, operator()));
    }

    @Test
    @DisplayName("终止开关关闭 → 拒绝（fail-closed）")
    void terminateDisabled_rejected() {
        ReflectionTestUtils.setField(service, "terminateEnabled", false);

        assertThrows(BusinessKeyProvisioningTerminateDisabledException.class,
                () -> service.terminateStale(command(), operator()));
    }

    @Test
    @DisplayName("自由文本：换行清理、超长截断、敏感信息过滤")
    void reason_sanitized() {
        String dirty = "证书已升级\n换行内容 token=abc123secret " + "A".repeat(400);
        String sanitized = service.sanitizeReason(dirty);
        assertNotNull(sanitized);
        assertFalse(sanitized.contains("\n"));
        assertTrue(sanitized.length() <= 200);
        assertFalse(sanitized.contains("abc123secret"));
        assertTrue(sanitized.contains("REDACTED"));
    }

    // ================= §9.2 并发语义（状态模拟） =================

    @Test
    @DisplayName("终止与原 requestId 重试并发：若重试已推进批次（终态化），终止幂等返回，不重新派生")
    void concurrentWithRetry_onlyOneAdvance() {
        // 模拟重试线程先提交：批次已 FAILED
        batchRef = newBatch(BusinessKeyProvisioningState.FAILED, LocalDateTime.now().minusHours(2));
        when(provisioningRepository.selectByRequestId(REQUEST_ID)).thenReturn(batchRef);
        when(provisioningRepository.selectByRequestIdForUpdate(REQUEST_ID)).thenReturn(batchRef);

        ProvisioningTerminationResult result = service.terminateStale(command(), operator());

        assertFalse(result.isChanged());
        // 重试侧不得重新派生：批次终态、目录不激活（此处无目录写入）
        verify(deviceBusinessKeyRepository, never()).markPendingKeysFailed(anyString(), anyList(), anyString());
    }

    @Test
    @DisplayName("终止与 ACK 并发：合法 ACK 先提交（COMPLETED）则终止拒绝且不激活目录")
    void concurrentWithAck_ackWins_terminateRejected() {
        // ACK 先提交 → COMPLETED；目录已是 ACTIVE
        batchRef = newBatch(BusinessKeyProvisioningState.COMPLETED, LocalDateTime.now().minusHours(2));
        when(provisioningRepository.selectByRequestId(REQUEST_ID)).thenReturn(batchRef);
        when(provisioningRepository.selectByRequestIdForUpdate(REQUEST_ID)).thenReturn(batchRef);
        TerminateStaleProvisioningCommand cmd = command();
        cmd.setExpectedState(BusinessKeyProvisioningState.COMPLETED);

        assertThrows(BusinessKeyProvisioningTerminateNotAllowedException.class,
                () -> service.terminateStale(cmd, operator()));
        verify(deviceBusinessKeyRepository, never()).markPendingKeysFailed(anyString(), anyList(), anyString());
    }

    @Test
    @DisplayName("两个终止并发：一次变更、一次幂等返回")
    void twoTerminateConcurrent_oneChanged_oneIdempotent() {
        List<DeviceBusinessKey> directories = new ArrayList<>(
                List.of(newPendingKey("UPLINK_AEAD", 1L, 1L), newPendingKey("DOWNLINK_AEAD", 2L, 2L)));
        List<BusinessKeyProvisioningItem> items = new ArrayList<>(
                List.of(newItem(1L, 1L, "UPLINK_AEAD", null), newItem(2L, 2L, "DOWNLINK_AEAD", null)));
        setUpStuckPreparing(items, directories);

        ProvisioningTerminationResult first = service.terminateStale(command(), operator());
        assertTrue(first.isChanged());

        // 第二次：批次已 FAILED → 幂等
        ProvisioningTerminationResult second = service.terminateStale(command(), operator());
        assertFalse(second.isChanged());
        assertEquals("FAILED", second.getAfterState());
    }

    // ================= 诊断只读 =================

    @Test
    @DisplayName("诊断：PREPARING 无副作用可清理，返回页面字段")
    void diagnose_clearable() {
        List<DeviceBusinessKey> directories = List.of(
                newPendingKey("UPLINK_AEAD", 1L, 1L), newPendingKey("DOWNLINK_AEAD", 2L, 2L));
        List<BusinessKeyProvisioningItem> items = List.of(
                newItem(1L, 1L, "UPLINK_AEAD", null), newItem(2L, 2L, "DOWNLINK_AEAD", null));
        batchRef = newBatch(BusinessKeyProvisioningState.PREPARING, LocalDateTime.now().minusHours(2));
        when(provisioningRepository.selectByRequestId(REQUEST_ID)).thenReturn(batchRef);
        when(provisioningItemRepository.selectByProvisioningId(1L)).thenReturn(items);
        items.forEach(i -> when(deviceBusinessKeyRepository.selectById(i.getDirectoryKeyId()))
                .thenReturn(directories.stream().filter(d -> d.getId().equals(i.getDirectoryKeyId())).findFirst().orElse(null)));
        when(operationEvidence.query(anyList())).thenReturn(Map.of(
                1L, ProvisioningOperationEvidence.OperationOutcome.NOT_STARTED,
                2L, ProvisioningOperationEvidence.OperationOutcome.NOT_STARTED));
        when(vmdOutboxRepository.selectList(any())).thenReturn(List.of());

        ProvisioningBlockingDiagnosisResult diag = service.inspectTermination(REQUEST_ID);

        assertTrue(diag.getClearable());
        assertEquals("CLEARABLE", diag.getSuggestedAction());
        assertEquals(2, diag.getPendingDirectoryCount());
        assertTrue(diag.getUniqueConstraintOccupied());
        assertEquals(2, diag.getOperationSummary().getNotStarted());
        assertNotNull(diag.getStaleDurationSeconds());
    }

    @Test
    @DisplayName("诊断：WAIT_DEVICE_ACK → WAIT_ACK_EXPIRY，不可清理")
    void diagnose_waitAckExpiry() {
        batchRef = newBatch(BusinessKeyProvisioningState.WAIT_DEVICE_ACK, LocalDateTime.now().minusHours(2));
        when(provisioningRepository.selectByRequestId(REQUEST_ID)).thenReturn(batchRef);
        when(provisioningItemRepository.selectByProvisioningId(1L)).thenReturn(List.of());
        when(operationEvidence.query(anyList())).thenReturn(Map.of());

        ProvisioningBlockingDiagnosisResult diag = service.inspectTermination(REQUEST_ID);

        assertFalse(diag.getClearable());
        assertEquals("WAIT_ACK_EXPIRY", diag.getSuggestedAction());
    }
}
