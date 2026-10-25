package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.KeyprovEnvelopeIntegrityException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.BusinessKeyProvisioningRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.BusinessKeyProvisioningMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link KeyProvisioningResponseStore} 单元测试（VMD-DSN-CR-068 §4/§7.3 字节级幂等重放）。
 * <p>
 * 覆盖：发送前持久化（加密 BLOB + digest + codec + persisted_at）、按 requestId 原字节重放、
 * 重放字节完整性校验失败 fail-closed、密钥缺失/非法 fail-closed。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
class KeyProvisioningResponseStoreTest {

    private BusinessKeyProvisioningRepository repository;
    private KeyProvisioningResponseStore store;
    private BusinessKeyProvisioning batch;

    @BeforeEach
    void setUp() {
        repository = mock(BusinessKeyProvisioningRepository.class);
        store = new KeyProvisioningResponseStore(repository, mock(BusinessKeyProvisioningMetrics.class));
        ReflectionTestUtils.setField(store, "base64EncryptionKey",
                Base64.getEncoder().encodeToString(new byte[32]));
        batch = BusinessKeyProvisioning.builder().id(1L).requestId("CR068-REQ-0001").build();
        when(repository.selectByRequestId("CR068-REQ-0001")).thenReturn(batch);
        when(repository.update(any())).thenReturn(1);
    }

    @Test
    @DisplayName("生产就绪：密钥已注入 → true；未注入 → false")
    void productionReady() {
        assertTrue(store.productionReady());
        ReflectionTestUtils.setField(store, "base64EncryptionKey", "");
        assertFalse(store.productionReady());
    }

    @Test
    @DisplayName("发送前持久化 → 加密 BLOB + digest + codec + persisted_at 落批次表")
    void persistWritesBatch() {
        byte[] response = "final-response-bytes".getBytes();
        store.persist("CR068-REQ-0001", response);
        assertEquals(ParProtoWireCanonical.AAD_PROFILE, batch.getResponseCodecVersion());
        assertNotNull(batch.getResponseDigest());
        assertNotNull(batch.getResponsePersistedAt());
        // 落库的是密文（非明文），且可解密回原文
        assertNotNull(batch.getResponsePayload());
        assertFalse(new String(batch.getResponsePayload()).contains("final-response"));
        assertArrayEquals(response, store.load("CR068-REQ-0001"));
    }

    @Test
    @DisplayName("同 requestId 重放返回原 bytes（不重新组装）")
    void replayReturnsOriginalBytes() {
        byte[] response = "stable-bytes-for-replay".getBytes();
        store.persist("CR068-REQ-0001", response);
        assertArrayEquals(response, store.load("CR068-REQ-0001"));
        assertArrayEquals(response, store.load("CR068-REQ-0001"), "重复重放必须字节级一致");
    }

    @Test
    @DisplayName("批次不存在或无 payload → load 返回 null（无重放事实）")
    void loadMissingReturnsNull() {
        when(repository.selectByRequestId("missing")).thenReturn(null);
        assertNull(store.load("missing"));
        BusinessKeyProvisioning noPayload = BusinessKeyProvisioning.builder().id(2L).requestId("no-payload").build();
        when(repository.selectByRequestId("no-payload")).thenReturn(noPayload);
        assertNull(store.load("no-payload"));
    }

    @Test
    @DisplayName("重放字节完整性校验失败（digest 不一致）→ fail-closed 拒绝")
    void replayDigestMismatchRejected() {
        byte[] response = "tampered-replay".getBytes();
        store.persist("CR068-REQ-0001", response);
        batch.setResponseDigest("deadbeef"); // 篡改 digest
        assertThrows(KeyprovEnvelopeIntegrityException.class, () -> store.load("CR068-REQ-0001"));
    }

    @Test
    @DisplayName("密钥未配置 → 持久化/重放均 fail-closed")
    void missingKeyFailClosed() {
        ReflectionTestUtils.setField(store, "base64EncryptionKey", "");
        assertThrows(KeyprovEnvelopeIntegrityException.class,
                () -> store.persist("CR068-REQ-0001", "x".getBytes()));
    }

    @Test
    @DisplayName("Response 字节为空 → 拒绝持久化")
    void emptyPayloadRejected() {
        assertThrows(KeyprovEnvelopeIntegrityException.class,
                () -> store.persist("CR068-REQ-0001", new byte[0]));
    }
}
