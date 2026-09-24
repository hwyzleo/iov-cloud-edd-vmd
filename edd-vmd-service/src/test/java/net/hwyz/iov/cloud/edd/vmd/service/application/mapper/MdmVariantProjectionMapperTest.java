package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.mdm.api.vo.response.VariantResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VariantProjectionCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmVariantEvent;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VariantProjectionException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Variant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVariantRepository;
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
 * MdmVariantProjectionMapper单元测试（CR-048 / RD-048-3 统一投影内核）
 */
@ExtendWith(MockitoExtension.class)
class MdmVariantProjectionMapperTest {

    @Mock
    private MdmVariantRepository mdmVariantRepository;

    @InjectMocks
    private MdmVariantProjectionMapper mapper;

    private VariantProjectionCommand validCommand() {
        return VariantProjectionCommand.builder()
                .code("VAR001")
                .name("版本1")
                .nameLocal("版本1本地化")
                .modelCode("MODEL001")
                .description("desc")
                .externalRefId("mdm-var-001")
                .externalVersion(1L)
                .occurredAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("fromSnapshot应将MDM快照映射为投影命令（不含平台/车系冗余）")
    void fromSnapshot_shouldMapMdmSnapshotToCommand() {
        // Given
        VariantResponse snapshot = VariantResponse.builder()
                .id(1001L).code("VAR001").name("版本1").nameLocal("版本1本地化")
                .modelCode("MODEL001").description("desc")
                .sourceId("mdm-var-001").version(2)
                .build();

        // When
        VariantProjectionCommand command = mapper.fromSnapshot(snapshot);

        // Then
        assertNotNull(command);
        assertEquals("VAR001", command.getCode());
        assertEquals("版本1", command.getName());
        assertEquals("版本1本地化", command.getNameLocal());
        assertEquals("MODEL001", command.getModelCode());
        assertEquals("mdm-var-001", command.getExternalRefId());
        assertEquals(2L, command.getExternalVersion());
    }

    @Test
    @DisplayName("fromEvent应将MDM事件映射为投影命令")
    void fromEvent_shouldMapMdmEventToCommand() {
        // Given
        MdmVariantEvent event = new MdmVariantEvent("UPDATED", "mdm-var-001", 3L, "VAR001",
                "版本1", "MODEL001", LocalDateTime.now());

        // When
        VariantProjectionCommand command = mapper.fromEvent(event);

        // Then
        assertNotNull(command);
        assertEquals("VAR001", command.getCode());
        assertEquals("版本1", command.getName());
        assertEquals("MODEL001", command.getModelCode());
        assertEquals("mdm-var-001", command.getExternalRefId());
        assertEquals(3L, command.getExternalVersion());
    }

    @Test
    @DisplayName("validate应通过含全部必需字段的命令")
    void validate_shouldPassWhenAllRequiredFieldsPresent() {
        mapper.validate(validCommand());
    }

    @Test
    @DisplayName("validate应拒绝缺少modelCode的命令（契约错误，不写半条投影）")
    void validate_shouldRejectMissingModelCode() {
        VariantProjectionCommand command = validCommand();
        command.setModelCode(null);

        VariantProjectionException exception = assertThrows(VariantProjectionException.class,
                () -> mapper.validate(command));

        assertTrue(exception.getMessage().contains("modelCode"));
    }

    @Test
    @DisplayName("apply应新增本地不存在的版本投影")
    void apply_shouldInsertWhenLocalVariantNotExists() {
        VariantProjectionCommand command = validCommand();

        when(mdmVariantRepository.selectByExternalRefId("mdm-var-001")).thenReturn(null);
        when(mdmVariantRepository.selectByCode("VAR001")).thenReturn(null);

        mapper.apply(command);

        verify(mdmVariantRepository).insert(argThat(variant ->
                "VAR001".equals(variant.getCode())
                        && "版本1本地化".equals(variant.getNameLocal())
                        && "MODEL001".equals(variant.getModelCode())
                        && SourceType.MDM == variant.getSource()
                        && 1L == variant.getExternalVersion()));
        verify(mdmVariantRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("apply应更新本地已存在且版本更高的版本投影")
    void apply_shouldUpdateWhenLocalVariantExistsAndVersionHigher() {
        VariantProjectionCommand command = validCommand();
        command.setExternalVersion(2L);

        Variant local = Variant.builder()
                .id(1L).code("VAR001").name("旧版本").modelCode("MODEL001")
                .source(SourceType.MDM).externalRefId("mdm-var-001").externalVersion(1L).build();

        when(mdmVariantRepository.selectByExternalRefId("mdm-var-001")).thenReturn(local);

        mapper.apply(command);

        verify(mdmVariantRepository).updateById(argThat(variant ->
                "版本1".equals(variant.getName()) && 2L == variant.getExternalVersion()));
        verify(mdmVariantRepository, never()).insert(any());
    }

    @Test
    @DisplayName("apply应忽略版本不高于本地的版本投影")
    void apply_shouldIgnoreWhenVersionNotHigher() {
        VariantProjectionCommand command = validCommand();
        command.setExternalVersion(1L);

        Variant local = Variant.builder()
                .id(1L).code("VAR001").name("版本1").modelCode("MODEL001")
                .source(SourceType.MDM).externalRefId("mdm-var-001").externalVersion(2L).build();

        when(mdmVariantRepository.selectByExternalRefId("mdm-var-001")).thenReturn(local);

        mapper.apply(command);

        verify(mdmVariantRepository, never()).insert(any());
        verify(mdmVariantRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("apply应external_ref_id优先、code兜底")
    void apply_shouldFallbackToCodeWhenExternalRefIdNotFound() {
        VariantProjectionCommand command = validCommand();

        when(mdmVariantRepository.selectByExternalRefId("mdm-var-001")).thenReturn(null);
        when(mdmVariantRepository.selectByCode("VAR001")).thenReturn(null);

        mapper.apply(command);

        verify(mdmVariantRepository).selectByCode("VAR001");
        verify(mdmVariantRepository).insert(any());
    }

    @Test
    @DisplayName("handleDeletion应逻辑删除已存在的版本投影")
    void handleDeletion_shouldLogicalDeleteWhenLocalExists() {
        MdmVariantEvent event = new MdmVariantEvent("DELETED", "mdm-var-001", 2L, "VAR001",
                null, "MODEL001", LocalDateTime.now());

        Variant local = Variant.builder()
                .id(1L).code("VAR001").name("版本1").source(SourceType.MDM)
                .externalRefId("mdm-var-001").externalVersion(1L).build();

        when(mdmVariantRepository.selectByCode("VAR001")).thenReturn(local);

        mapper.handleDeletion(event);

        verify(mdmVariantRepository).logicalDeleteById(1L);
    }

    @Test
    @DisplayName("handleDeletion应忽略本地投影不存在的删除事件")
    void handleDeletion_shouldIgnoreWhenLocalNotExists() {
        MdmVariantEvent event = new MdmVariantEvent("DELETED", "mdm-var-001", 2L, "VAR001",
                null, "MODEL001", LocalDateTime.now());

        when(mdmVariantRepository.selectByCode("VAR001")).thenReturn(null);
        when(mdmVariantRepository.selectByExternalRefId("mdm-var-001")).thenReturn(null);

        mapper.handleDeletion(event);

        verify(mdmVariantRepository, never()).logicalDeleteById(any());
    }

    @Test
    @DisplayName("handleDeletion应忽略旧版本删除事件")
    void handleDeletion_shouldIgnoreWhenVersionNotHigher() {
        MdmVariantEvent event = new MdmVariantEvent("DELETED", "mdm-var-001", 1L, "VAR001",
                null, "MODEL001", LocalDateTime.now());

        Variant local = Variant.builder()
                .id(1L).code("VAR001").name("版本1").source(SourceType.MDM)
                .externalRefId("mdm-var-001").externalVersion(2L).build();

        when(mdmVariantRepository.selectByCode("VAR001")).thenReturn(local);

        mapper.handleDeletion(event);

        verify(mdmVariantRepository, never()).logicalDeleteById(any());
    }
}
