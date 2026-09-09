package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.mdm.api.vo.response.ConfigurationResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationProjectionCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmConfigurationEvent;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ConfigurationProjectionException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmConfigurationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.ConfigurationSyncMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MdmConfigurationProjectionMapper单元测试（CR-047 / RD-047-3 统一投影内核）
 */
@ExtendWith(MockitoExtension.class)
class MdmConfigurationProjectionMapperTest {

    @Mock
    private MdmConfigurationRepository mdmConfigurationRepository;

    @Mock
    private ConfigurationSyncMetrics configurationSyncMetrics;

    @InjectMocks
    private MdmConfigurationProjectionMapper mapper;

    private ConfigurationProjectionCommand validCommand() {
        return ConfigurationProjectionCommand.builder()
                .code("CFG001")
                .name("配置1")
                .nameLocal("配置1本地化")
                .variantCode("VAR001")
                .description("desc")
                .externalRefId("mdm-cfg-001")
                .externalVersion(1L)
                .occurredAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("fromSnapshot应将MDM快照映射为投影命令")
    void fromSnapshot_shouldMapMdmSnapshotToCommand() {
        // Given
        ConfigurationResponse snapshot = ConfigurationResponse.builder()
                .id(1001L).code("CFG001").name("配置1").nameLocal("配置1本地化")
                .variantCode("VAR001").description("desc").sourceId("mdm-cfg-001").version(2)
                .build();

        // When
        ConfigurationProjectionCommand command = mapper.fromSnapshot(snapshot);

        // Then
        assertNotNull(command);
        assertEquals("CFG001", command.getCode());
        assertEquals("配置1", command.getName());
        assertEquals("配置1本地化", command.getNameLocal());
        assertEquals("VAR001", command.getVariantCode());
        assertEquals("mdm-cfg-001", command.getExternalRefId());
        assertEquals(2L, command.getExternalVersion());
    }

    @Test
    @DisplayName("fromEvent应将MDM事件映射为投影命令")
    void fromEvent_shouldMapMdmEventToCommand() {
        // Given
        MdmConfigurationEvent event = new MdmConfigurationEvent("UPDATED", "mdm-cfg-001", 3L, "CFG001",
                "配置1", "配置1本地化", "VAR001", "desc", LocalDateTime.now());

        // When
        ConfigurationProjectionCommand command = mapper.fromEvent(event);

        // Then
        assertNotNull(command);
        assertEquals("CFG001", command.getCode());
        assertEquals("配置1", command.getName());
        assertEquals("VAR001", command.getVariantCode());
        assertEquals("mdm-cfg-001", command.getExternalRefId());
        assertEquals(3L, command.getExternalVersion());
    }

    @Test
    @DisplayName("validate应通过含全部必需字段的命令")
    void validate_shouldPassWhenAllRequiredFieldsPresent() {
        // Given
        ConfigurationProjectionCommand command = validCommand();

        // When
        mapper.validate(command);

        // Then
        // no exception
    }

    @Test
    @DisplayName("validate应拒绝缺少variantCode的命令（契约错误，不写半条投影）")
    void validate_shouldRejectMissingVariantCode() {
        // Given
        ConfigurationProjectionCommand command = validCommand();
        command.setVariantCode(null);

        // When
        ConfigurationProjectionException exception = assertThrows(ConfigurationProjectionException.class,
                () -> mapper.validate(command));

        // Then
        assertTrue(exception.getMessage().contains("variantCode"));
    }

    @Test
    @DisplayName("validate应拒绝缺少code的命令")
    void validate_shouldRejectMissingCode() {
        // Given
        ConfigurationProjectionCommand command = validCommand();
        command.setCode(null);

        // When
        assertThrows(ConfigurationProjectionException.class, () -> mapper.validate(command));
    }

    @Test
    @DisplayName("validate应拒绝缺少externalRefId的命令")
    void validate_shouldRejectMissingExternalRefId() {
        // Given
        ConfigurationProjectionCommand command = validCommand();
        command.setExternalRefId(null);

        // When
        assertThrows(ConfigurationProjectionException.class, () -> mapper.validate(command));
    }

    @Test
    @DisplayName("validate应拒绝缺少version的命令")
    void validate_shouldRejectMissingVersion() {
        // Given
        ConfigurationProjectionCommand command = validCommand();
        command.setExternalVersion(null);

        // When
        assertThrows(ConfigurationProjectionException.class, () -> mapper.validate(command));
    }

    @Test
    @DisplayName("apply应新增本地不存在的投影（externalRefId/code 均未命中）")
    void apply_shouldInsertWhenLocalNotExists() {
        // Given
        ConfigurationProjectionCommand command = validCommand();
        when(mdmConfigurationRepository.selectByExternalRefId("mdm-cfg-001")).thenReturn(null);
        when(mdmConfigurationRepository.selectByCode("CFG001")).thenReturn(null);
        when(mdmConfigurationRepository.insert(any(Configuration.class))).thenReturn(1);

        // When
        mapper.apply(command);

        // Then
        verify(mdmConfigurationRepository).insert(any(Configuration.class));
        verify(mdmConfigurationRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("apply应按externalRefId命中并更新（版本更高）")
    void apply_shouldUpdateWhenExternalRefIdMatchedWithHigherVersion() {
        // Given
        ConfigurationProjectionCommand command = validCommand();
        command.setExternalVersion(2L);
        Configuration local = Configuration.builder()
                .id(1L).code("CFG001").name("旧名称").variantCode("VAR001")
                .source(SourceType.MDM).externalRefId("mdm-cfg-001").externalVersion(1L).build();
        when(mdmConfigurationRepository.selectByExternalRefId("mdm-cfg-001")).thenReturn(local);
        when(mdmConfigurationRepository.updateById(local)).thenReturn(1);

        // When
        mapper.apply(command);

        // Then
        assertEquals("配置1", local.getName());
        assertEquals(2L, local.getExternalVersion());
        verify(mdmConfigurationRepository).updateById(local);
        verify(mdmConfigurationRepository, never()).insert(any());
    }

    @Test
    @DisplayName("apply应忽略旧版本事件并计数（版本门禁）")
    void apply_shouldIgnoreStaleEventAndCount() {
        // Given
        ConfigurationProjectionCommand command = validCommand();
        command.setExternalVersion(1L);
        Configuration local = Configuration.builder()
                .id(1L).code("CFG001").name("配置1").variantCode("VAR001")
                .source(SourceType.MDM).externalRefId("mdm-cfg-001").externalVersion(5L).build();
        when(mdmConfigurationRepository.selectByExternalRefId("mdm-cfg-001")).thenReturn(local);

        // When
        mapper.apply(command);

        // Then
        verify(configurationSyncMetrics).recordStaleEvent();
        verify(mdmConfigurationRepository, never()).updateById(any());
        verify(mdmConfigurationRepository, never()).insert(any());
    }

    @Test
    @DisplayName("apply应externalRefId优先、code兜底（externalRefId未命中时按code命中更新）")
    void apply_shouldFallbackToCodeWhenExternalRefIdNotMatched() {
        // Given
        ConfigurationProjectionCommand command = validCommand();
        command.setExternalVersion(3L);
        Configuration local = Configuration.builder()
                .id(1L).code("CFG001").name("旧名称").variantCode("VAR001")
                .source(SourceType.MDM).externalRefId("old-ext-id").externalVersion(2L).build();
        when(mdmConfigurationRepository.selectByExternalRefId("mdm-cfg-001")).thenReturn(null);
        when(mdmConfigurationRepository.selectByCode("CFG001")).thenReturn(local);
        when(mdmConfigurationRepository.updateById(local)).thenReturn(1);

        // When
        mapper.apply(command);

        // Then
        assertEquals("mdm-cfg-001", local.getExternalRefId());
        assertEquals(3L, local.getExternalVersion());
        verify(mdmConfigurationRepository).updateById(local);
    }

    @Test
    @DisplayName("apply应拒绝缺variantCode命令（契约错误）")
    void apply_shouldRejectCommandMissingVariantCode() {
        // Given
        ConfigurationProjectionCommand command = validCommand();
        command.setVariantCode(null);

        // When
        assertThrows(ConfigurationProjectionException.class, () -> mapper.apply(command));

        // Then
        verify(mdmConfigurationRepository, never()).insert(any());
        verify(mdmConfigurationRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("handleDeletion应按code命中并逻辑删除（版本更高）")
    void handleDeletion_shouldLogicalDeleteWhenLocalExistsWithHigherEventVersion() {
        // Given
        MdmConfigurationEvent event = new MdmConfigurationEvent("DELETED", "mdm-cfg-001", 6L, "CFG001",
                null, null, null, null, LocalDateTime.now());
        Configuration local = Configuration.builder()
                .id(1L).code("CFG001").name("配置1").variantCode("VAR001")
                .source(SourceType.MDM).externalRefId("mdm-cfg-001").externalVersion(5L).build();
        when(mdmConfigurationRepository.selectByCode("CFG001")).thenReturn(local);
        when(mdmConfigurationRepository.logicalDeleteById(1L)).thenReturn(1);

        // When
        mapper.handleDeletion(event);

        // Then
        verify(mdmConfigurationRepository).logicalDeleteById(1L);
        verify(configurationSyncMetrics).recordDeletedEvent();
    }

    @Test
    @DisplayName("handleDeletion应忽略旧版本删除事件并计数")
    void handleDeletion_shouldIgnoreStaleDeletionEvent() {
        // Given
        MdmConfigurationEvent event = new MdmConfigurationEvent("DEACTIVATED", "mdm-cfg-001", 1L, "CFG001",
                null, null, null, null, LocalDateTime.now());
        Configuration local = Configuration.builder()
                .id(1L).code("CFG001").name("配置1").variantCode("VAR001")
                .source(SourceType.MDM).externalRefId("mdm-cfg-001").externalVersion(5L).build();
        when(mdmConfigurationRepository.selectByCode("CFG001")).thenReturn(local);

        // When
        mapper.handleDeletion(event);

        // Then
        verify(configurationSyncMetrics).recordStaleEvent();
        verify(mdmConfigurationRepository, never()).logicalDeleteById(any());
    }

    @Test
    @DisplayName("handleDeletion应在本地投影不存在时安全返回")
    void handleDeletion_shouldReturnSafelyWhenLocalNotExists() {
        // Given
        MdmConfigurationEvent event = new MdmConfigurationEvent("DELETED", "mdm-cfg-001", 6L, "CFG001",
                null, null, null, null, LocalDateTime.now());
        when(mdmConfigurationRepository.selectByCode("CFG001")).thenReturn(null);
        when(mdmConfigurationRepository.selectByExternalRefId("mdm-cfg-001")).thenReturn(null);

        // When
        mapper.handleDeletion(event);

        // Then
        verify(mdmConfigurationRepository, never()).logicalDeleteById(any());
    }
}
