package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPartEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MDM零件同步集成测试
 * <p>
 * 测试真实数据库场景下的幂等性，包括：
 * 1. 正常新增和更新
 * 2. Bootstrap与Kafka增量同步的ID映射一致性
 * 3. 重复消息处理的幂等性
 * </p>
 */
@Transactional
class MdmPartSyncIntegrationTest extends BaseTest {

    @Autowired
    private MdmSyncAppService mdmSyncAppService;

    @Autowired
    private MdmPartRepository mdmPartRepository;

    private static final String TEST_PN = "TEST-PART-001";
    private static final String MDM_ENTITY_ID = "1001";
    private static final String MDM_CODE = TEST_PN;

    @BeforeEach
    void setUp() {
        // 由于使用@Transactional注解，每个测试方法会自动回滚
        // 但为了确保测试独立性，清理可能存在的测试数据
        Part existing = mdmPartRepository.selectByPn(TEST_PN);
        if (existing != null) {
            mdmPartRepository.batchPhysicalDelete(new Long[]{existing.getId()});
        }
    }

    @Test
    @DisplayName("Kafka增量同步应正确插入新零件")
    void handlePartEvent_shouldInsertNewPart() {
        // Given
        MdmPartEvent event = createEvent(MDM_ENTITY_ID, MDM_CODE, 1L);

        // When
        mdmSyncAppService.handlePartEvent(event);

        // Then
        Part saved = mdmPartRepository.selectByPn(TEST_PN);
        assertNotNull(saved, "零件应该被保存");
        assertEquals(TEST_PN, saved.getPn());
        assertEquals(MDM_ENTITY_ID, saved.getExternalRefId());
        assertEquals(1L, saved.getExternalVersion());
        assertEquals(SourceType.MDM, saved.getSource());
    }

    @Test
    @DisplayName("Kafka增量同步应正确更新已存在零件（版本更高）")
    void handlePartEvent_shouldUpdateExistingPartWhenVersionHigher() {
        // Given - 先插入
        MdmPartEvent event1 = createEvent(MDM_ENTITY_ID, MDM_CODE, 1L);
        mdmSyncAppService.handlePartEvent(event1);

        // When - 版本更高的事件
        MdmPartEvent event2 = createEvent(MDM_ENTITY_ID, MDM_CODE, 2L);
        event2.setName("更新后的零件名称");
        mdmSyncAppService.handlePartEvent(event2);

        // Then
        Part updated = mdmPartRepository.selectByPn(TEST_PN);
        assertNotNull(updated);
        assertEquals(2L, updated.getExternalVersion());
        assertEquals("更新后的零件名称", updated.getName());
    }

    @Test
    @DisplayName("Kafka增量同步应忽略版本不高的事件（幂等性）")
    void handlePartEvent_shouldIgnoreEventWithLowerOrEqualVersion() {
        // Given - 先插入版本2
        MdmPartEvent event1 = createEvent(MDM_ENTITY_ID, MDM_CODE, 2L);
        event1.setName("版本2的零件");
        mdmSyncAppService.handlePartEvent(event1);

        // When - 版本1的事件（应该被忽略）
        MdmPartEvent event2 = createEvent(MDM_ENTITY_ID, MDM_CODE, 1L);
        event2.setName("版本1的零件");
        mdmSyncAppService.handlePartEvent(event2);

        // Then - 应该保留版本2的数据
        Part part = mdmPartRepository.selectByPn(TEST_PN);
        assertNotNull(part);
        assertEquals(2L, part.getExternalVersion());
        assertEquals("版本2的零件", part.getName());
    }

    @Test
    @DisplayName("重复处理同一条消息应保持幂等")
    void handlePartEvent_shouldBeIdempotentForSameMessage() {
        // Given
        MdmPartEvent event = createEvent(MDM_ENTITY_ID, MDM_CODE, 1L);

        // When - 处理同一条消息两次
        mdmSyncAppService.handlePartEvent(event);
        mdmSyncAppService.handlePartEvent(event);

        // Then - 应该只有一条记录
        Part part = mdmPartRepository.selectByPn(TEST_PN);
        assertNotNull(part);
        assertEquals(MDM_ENTITY_ID, part.getExternalRefId());
        assertEquals(1L, part.getExternalVersion());
    }

    @Test
    @DisplayName("Bootstrap用code同步后，Kafka用id同步应能正确更新（ID映射一致性）")
    void handlePartEvent_shouldHandleIdMappingBetweenBootstrapAndKafka() {
        // Given - 模拟Bootstrap同步：用code作为external_ref_id
        Part bootstrappedPart = Part.builder()
                .pn(TEST_PN)
                .name("Bootstrap同步的零件")
                .source(SourceType.MDM)
                .externalRefId(MDM_CODE)  // Bootstrap用code作为external_ref_id
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();
        mdmPartRepository.insert(bootstrappedPart);

        // When - Kafka事件用id（MDM主键）作为entityId
        MdmPartEvent kafkaEvent = createEvent(MDM_ENTITY_ID, MDM_CODE, 2L);
        kafkaEvent.setName("Kafka更新的零件");

        // 这里会暴露问题：Kafka用MDM_ENTITY_ID查询，但Bootstrap用MDM_CODE存储
        // 如果代码没有处理好，会尝试insert导致唯一约束冲突
        try {
            mdmSyncAppService.handlePartEvent(kafkaEvent);

            // 如果成功，说明代码已经正确处理了ID映射
            Part updated = mdmPartRepository.selectByPn(TEST_PN);
            assertNotNull(updated);
            // 验证是更新而不是新增
            assertEquals(2L, updated.getExternalVersion());
        } catch (Exception e) {
            // 如果失败，说明存在ID映射问题
            fail("Bootstrap与Kafka的ID映射不一致，导致处理失败: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("连续处理多个不同版本的事件应保持最终状态正确")
    void handlePartEvent_shouldHandleMultipleVersionUpdatesCorrectly() {
        // Given & When - 按顺序处理版本1, 3, 2, 4
        mdmSyncAppService.handlePartEvent(createEvent(MDM_ENTITY_ID, MDM_CODE, 1L));
        mdmSyncAppService.handlePartEvent(createEvent(MDM_ENTITY_ID, MDM_CODE, 3L));
        mdmSyncAppService.handlePartEvent(createEvent(MDM_ENTITY_ID, MDM_CODE, 2L));
        mdmSyncAppService.handlePartEvent(createEvent(MDM_ENTITY_ID, MDM_CODE, 4L));

        // Then - 最终应该是版本4
        Part part = mdmPartRepository.selectByPn(TEST_PN);
        assertNotNull(part);
        assertEquals(4L, part.getExternalVersion());
    }

    private MdmPartEvent createEvent(String entityId, String code, Long version) {
        return new MdmPartEvent(
                "CREATED",
                entityId,
                version,
                code,
                "测试零件",
                "NORMAL",
                "NODE001",
                "SUPPLIER001",
                true,
                true,
                true,
                "PRODUCTION",
                LocalDateTime.now()
        );
    }
}
