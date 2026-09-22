package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.mdm.api.vo.response.VehicleNodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleNodeProjectionCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmVehicleNodeEvent;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleNodeProjectionException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MdmVehicleNodeProjectionMapper 单元测试（CR-049）
 * <p>
 * Bootstrap 快照与 Kafka 事件统一投影内核：映射、校验、版本门禁、幂等 upsert、旧版本忽略。
 * </p>
 *
 * @author hwyz_leo
 */
@DisplayName("MdmVehicleNodeProjectionMapper 测试")
class MdmVehicleNodeProjectionMapperTest {

    private MdmVehicleNodeRepository mdmVehicleNodeRepository;
    private MdmVehicleNodeProjectionMapper mapper;

    @BeforeEach
    void setUp() {
        mdmVehicleNodeRepository = mock(MdmVehicleNodeRepository.class);
        mapper = new MdmVehicleNodeProjectionMapper(mdmVehicleNodeRepository);
    }

    private MdmVehicleNodeEvent event(String code, String entityId, long version, String deviceCategory,
                                      String hsmCapability) {
        return new MdmVehicleNodeEvent("UPDATED", entityId, version, code,
                "节点", "Node", deviceCategory, "EEAD", "CONTROLLER", "FOTA",
                true, 1, hsmCapability, LocalDateTime.now());
    }

    @Test
    @DisplayName("fromEvent 应透传 hsmCapability/deviceCategory")
    void fromEvent_shouldMapHsmCapabilityAndDeviceCategory() {
        MdmVehicleNodeEvent event = event("CCU_GEN2", "mdm-vn-ccu2", 12L, "CCU", "HSM_FULL");

        VehicleNodeProjectionCommand command = mapper.fromEvent(event);

        assertEquals("CCU_GEN2", command.getCode());
        assertEquals("CCU", command.getDeviceCategory());
        assertEquals("HSM_FULL", command.getHsmCapability());
        assertEquals("mdm-vn-ccu2", command.getExternalRefId());
        assertEquals(12L, command.getExternalVersion());
    }

    @Test
    @DisplayName("fromSnapshot 应透传 hsmCapability/deviceCategory 并兜底默认字段")
    void fromSnapshot_shouldMapHsmCapabilityAndDefaults() {
        VehicleNodeResponse snapshot = VehicleNodeResponse.builder()
                .nodeCode("TBOX_5G")
                .name("车联终端")
                .deviceCategory("TBOX")
                .hsmCapability("HSM_LIGHT")
                .nodeType("CONTROLLER")
                .functionalDomain("EEAD")
                .otaSupportType("FOTA")
                .externalRefId("mdm-vn-tbox")
                .externalVersion(7L)
                .build();

        VehicleNodeProjectionCommand command = mapper.fromSnapshot(snapshot);

        assertEquals("TBOX_5G", command.getCode());
        assertEquals("TBOX", command.getDeviceCategory());
        assertEquals("HSM_LIGHT", command.getHsmCapability());
        assertEquals(7L, command.getExternalVersion());
        assertNotNull(command.getFuncDomain());
        assertNotNull(command.getNodeType());
    }

    @Test
    @DisplayName("apply 应新增不存在的投影并写入 hsmCapability/deviceCategory")
    void apply_shouldInsertWhenNotExists() {
        VehicleNodeProjectionCommand command = VehicleNodeProjectionCommand.builder()
                .code("CCU_GEN2").name("中央计算单元GEN2")
                .deviceCategory("CCU").hsmCapability("HSM_FULL")
                .externalRefId("mdm-vn-ccu2").externalVersion(12L)
                .build();
        when(mdmVehicleNodeRepository.selectByCode("CCU_GEN2")).thenReturn(null);
        when(mdmVehicleNodeRepository.insert(any())).thenReturn(1);

        mapper.apply(command);

        ArgumentCaptor<VehicleNode> captor = ArgumentCaptor.forClass(VehicleNode.class);
        verify(mdmVehicleNodeRepository).insert(captor.capture());
        VehicleNode inserted = captor.getValue();
        assertEquals("CCU_GEN2", inserted.getCode());
        assertEquals("CCU", inserted.getDeviceCategory());
        assertEquals("HSM_FULL", inserted.getHsmCapability());
        assertEquals(SourceType.MDM, inserted.getSource());
        assertEquals(12L, inserted.getExternalVersion());
    }

    @Test
    @DisplayName("apply 应更新已存在且版本更高的投影，透传 hsmCapability")
    void apply_shouldUpdateWhenVersionHigher() {
        VehicleNodeProjectionCommand command = VehicleNodeProjectionCommand.builder()
                .code("TBOX_5G").name("车联终端")
                .deviceCategory("TBOX").hsmCapability("HSM_FULL")
                .externalRefId("mdm-vn-tbox").externalVersion(8L)
                .build();
        VehicleNode local = VehicleNode.builder()
                .id(1L).code("TBOX_5G").name("旧名")
                .externalVersion(7L).source(SourceType.MDM)
                .build();
        when(mdmVehicleNodeRepository.selectByCode("TBOX_5G")).thenReturn(local);
        when(mdmVehicleNodeRepository.updateById(any())).thenReturn(1);

        mapper.apply(command);

        assertEquals("HSM_FULL", local.getHsmCapability());
        assertEquals("TBOX", local.getDeviceCategory());
        assertEquals(8L, local.getExternalVersion());
        verify(mdmVehicleNodeRepository).updateById(local);
    }

    @Test
    @DisplayName("apply 应忽略旧版本事件")
    void apply_shouldIgnoreStaleVersion() {
        VehicleNodeProjectionCommand command = VehicleNodeProjectionCommand.builder()
                .code("TBOX_5G").name("车联终端")
                .deviceCategory("TBOX").hsmCapability("HSM_FULL")
                .externalRefId("mdm-vn-tbox").externalVersion(5L)
                .build();
        VehicleNode local = VehicleNode.builder()
                .id(1L).code("TBOX_5G").name("旧名")
                .externalVersion(7L).source(SourceType.MDM)
                .build();
        when(mdmVehicleNodeRepository.selectByCode("TBOX_5G")).thenReturn(local);

        mapper.apply(command);

        verify(mdmVehicleNodeRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("apply 缺失必需字段应抛契约异常，不写半条投影")
    void apply_shouldThrowOnMissingRequiredFields() {
        VehicleNodeProjectionCommand noRef = VehicleNodeProjectionCommand.builder()
                .code("VN001").externalVersion(1L).build();
        assertThrows(VehicleNodeProjectionException.class, () -> mapper.apply(noRef));

        VehicleNodeProjectionCommand noVersion = VehicleNodeProjectionCommand.builder()
                .code("VN001").externalRefId("mdm-vn-001").build();
        assertThrows(VehicleNodeProjectionException.class, () -> mapper.apply(noVersion));

        verify(mdmVehicleNodeRepository, never()).insert(any());
        verify(mdmVehicleNodeRepository, never()).updateById(any());
    }
}
