package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.mdm.api.vo.response.ModelResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ModelProjectionCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmModelEvent;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ModelProjectionException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmModelRepository;
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
 * MdmModelProjectionMapper单元测试（CR-048 / RD-048-3 统一投影内核）
 */
@ExtendWith(MockitoExtension.class)
class MdmModelProjectionMapperTest {

    @Mock
    private MdmModelRepository mdmModelRepository;

    @InjectMocks
    private MdmModelProjectionMapper mapper;

    private ModelProjectionCommand validCommand() {
        return ModelProjectionCommand.builder()
                .code("MODEL001")
                .name("车型1")
                .nameLocal("车型1本地化")
                .carLineCode("CARLINE001")
                .platformCode("PLATFORM001")
                .description("desc")
                .externalRefId("mdm-model-001")
                .externalVersion(1L)
                .occurredAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("fromSnapshot应将MDM快照映射为投影命令")
    void fromSnapshot_shouldMapMdmSnapshotToCommand() {
        // Given
        ModelResponse snapshot = ModelResponse.builder()
                .id(1001L).code("MODEL001").name("车型1").nameLocal("车型1本地化")
                .carLineCode("CARLINE001").platformCode("PLATFORM001").description("desc")
                .sourceId("mdm-model-001").version(2)
                .build();

        // When
        ModelProjectionCommand command = mapper.fromSnapshot(snapshot);

        // Then
        assertNotNull(command);
        assertEquals("MODEL001", command.getCode());
        assertEquals("车型1", command.getName());
        assertEquals("车型1本地化", command.getNameLocal());
        assertEquals("CARLINE001", command.getCarLineCode());
        assertEquals("PLATFORM001", command.getPlatformCode());
        assertEquals("mdm-model-001", command.getExternalRefId());
        assertEquals(2L, command.getExternalVersion());
    }

    @Test
    @DisplayName("fromEvent应将MDM事件映射为投影命令")
    void fromEvent_shouldMapMdmEventToCommand() {
        // Given
        MdmModelEvent event = new MdmModelEvent("UPDATED", "mdm-model-001", 3L, "MODEL001",
                "车型1", "PLATFORM001", "CARLINE001", LocalDateTime.now());

        // When
        ModelProjectionCommand command = mapper.fromEvent(event);

        // Then
        assertNotNull(command);
        assertEquals("MODEL001", command.getCode());
        assertEquals("车型1", command.getName());
        assertEquals("PLATFORM001", command.getPlatformCode());
        assertEquals("CARLINE001", command.getCarLineCode());
        assertEquals("mdm-model-001", command.getExternalRefId());
        assertEquals(3L, command.getExternalVersion());
    }

    @Test
    @DisplayName("validate应通过含全部必需字段的命令")
    void validate_shouldPassWhenAllRequiredFieldsPresent() {
        mapper.validate(validCommand());
    }

    @Test
    @DisplayName("validate应拒绝缺少platformCode的命令（契约错误，不写半条投影）")
    void validate_shouldRejectMissingPlatformCode() {
        ModelProjectionCommand command = validCommand();
        command.setPlatformCode(null);

        ModelProjectionException exception = assertThrows(ModelProjectionException.class,
                () -> mapper.validate(command));

        assertTrue(exception.getMessage().contains("platformCode"));
    }

    @Test
    @DisplayName("apply应新增本地不存在的车型投影")
    void apply_shouldInsertWhenLocalModelNotExists() {
        ModelProjectionCommand command = validCommand();

        when(mdmModelRepository.selectByExternalRefId("mdm-model-001")).thenReturn(null);
        when(mdmModelRepository.selectByCode("MODEL001")).thenReturn(null);

        mapper.apply(command);

        verify(mdmModelRepository).insert(argThat(model ->
                "MODEL001".equals(model.getCode())
                        && "车型1本地化".equals(model.getNameLocal())
                        && "PLATFORM001".equals(model.getPlatformCode())
                        && "CARLINE001".equals(model.getCarLineCode())
                        && SourceType.MDM == model.getSource()
                        && 1L == model.getExternalVersion()));
        verify(mdmModelRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("apply应更新本地已存在且版本更高的车型投影")
    void apply_shouldUpdateWhenLocalModelExistsAndVersionHigher() {
        ModelProjectionCommand command = validCommand();
        command.setExternalVersion(2L);

        Model local = Model.builder()
                .id(1L).code("MODEL001").name("旧车型").platformCode("PLATFORM001")
                .carLineCode("CARLINE001").source(SourceType.MDM)
                .externalRefId("mdm-model-001").externalVersion(1L).build();

        when(mdmModelRepository.selectByExternalRefId("mdm-model-001")).thenReturn(local);

        mapper.apply(command);

        verify(mdmModelRepository).updateById(argThat(model ->
                "车型1".equals(model.getName()) && 2L == model.getExternalVersion()));
        verify(mdmModelRepository, never()).insert(any());
    }

    @Test
    @DisplayName("apply应忽略版本不高于本地的车型投影")
    void apply_shouldIgnoreWhenVersionNotHigher() {
        ModelProjectionCommand command = validCommand();
        command.setExternalVersion(1L);

        Model local = Model.builder()
                .id(1L).code("MODEL001").name("车型1").platformCode("PLATFORM001")
                .carLineCode("CARLINE001").source(SourceType.MDM)
                .externalRefId("mdm-model-001").externalVersion(2L).build();

        when(mdmModelRepository.selectByExternalRefId("mdm-model-001")).thenReturn(local);

        mapper.apply(command);

        verify(mdmModelRepository, never()).insert(any());
        verify(mdmModelRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("apply应external_ref_id优先、code兜底")
    void apply_shouldFallbackToCodeWhenExternalRefIdNotFound() {
        ModelProjectionCommand command = validCommand();

        when(mdmModelRepository.selectByExternalRefId("mdm-model-001")).thenReturn(null);
        when(mdmModelRepository.selectByCode("MODEL001")).thenReturn(null);

        mapper.apply(command);

        verify(mdmModelRepository).selectByCode("MODEL001");
        verify(mdmModelRepository).insert(any());
    }

    @Test
    @DisplayName("handleDeletion应逻辑删除已存在的车型投影")
    void handleDeletion_shouldLogicalDeleteWhenLocalExists() {
        MdmModelEvent event = new MdmModelEvent("DELETED", "mdm-model-001", 2L, "MODEL001",
                null, null, null, LocalDateTime.now());

        Model local = Model.builder()
                .id(1L).code("MODEL001").name("车型1").source(SourceType.MDM)
                .externalRefId("mdm-model-001").externalVersion(1L).build();

        when(mdmModelRepository.selectByCode("MODEL001")).thenReturn(local);

        mapper.handleDeletion(event);

        verify(mdmModelRepository).logicalDeleteById(1L);
    }

    @Test
    @DisplayName("handleDeletion应忽略本地投影不存在的删除事件")
    void handleDeletion_shouldIgnoreWhenLocalNotExists() {
        MdmModelEvent event = new MdmModelEvent("DELETED", "mdm-model-001", 2L, "MODEL001",
                null, null, null, LocalDateTime.now());

        when(mdmModelRepository.selectByCode("MODEL001")).thenReturn(null);
        when(mdmModelRepository.selectByExternalRefId("mdm-model-001")).thenReturn(null);

        mapper.handleDeletion(event);

        verify(mdmModelRepository, never()).logicalDeleteById(any());
    }

    @Test
    @DisplayName("handleDeletion应忽略旧版本删除事件")
    void handleDeletion_shouldIgnoreWhenVersionNotHigher() {
        MdmModelEvent event = new MdmModelEvent("DELETED", "mdm-model-001", 1L, "MODEL001",
                null, null, null, LocalDateTime.now());

        Model local = Model.builder()
                .id(1L).code("MODEL001").name("车型1").source(SourceType.MDM)
                .externalRefId("mdm-model-001").externalVersion(2L).build();

        when(mdmModelRepository.selectByCode("MODEL001")).thenReturn(local);

        mapper.handleDeletion(event);

        verify(mdmModelRepository, never()).logicalDeleteById(any());
    }
}
