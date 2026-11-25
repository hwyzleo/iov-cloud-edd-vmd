package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.mdm.api.vo.response.VehicleNodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleNodeProjectionCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmVehicleNodeEvent;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleNodeProjectionException;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartCategoryAutoConvergenceService;
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
    private PartCategoryAutoConvergenceService partCategoryAutoConvergenceService;
    private MdmVehicleNodeProjectionMapper mapper;

    @BeforeEach
    void setUp() {
        mdmVehicleNodeRepository = mock(MdmVehicleNodeRepository.class);
        partCategoryAutoConvergenceService = mock(PartCategoryAutoConvergenceService.class);
        mapper = new MdmVehicleNodeProjectionMapper(mdmVehicleNodeRepository, partCategoryAutoConvergenceService);
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

    @Test
    @DisplayName("CR-078 TC-VMD-78-04：类别变更应触发零件派生类别自动收敛")
    void apply_shouldTriggerConvergenceWhenCategoryChanged() {
        VehicleNodeProjectionCommand command = VehicleNodeProjectionCommand.builder()
                .code("DCU_COCKPIT_SA8295P").name("座舱域控")
                .deviceCategory("IVI").hsmCapability("HSM_FULL")
                .externalRefId("mdm-vn-dcu").externalVersion(3L)
                .build();
        VehicleNode local = VehicleNode.builder()
                .id(2L).code("DCU_COCKPIT_SA8295P").name("座舱域控")
                .deviceCategory("DCU").externalVersion(2L).source(SourceType.MDM)
                .build();
        when(mdmVehicleNodeRepository.selectByCode("DCU_COCKPIT_SA8295P")).thenReturn(local);
        when(mdmVehicleNodeRepository.updateById(any())).thenReturn(1);

        mapper.apply(command);

        verify(partCategoryAutoConvergenceService)
                .onNodeCategoryChanged("DCU_COCKPIT_SA8295P", "DCU", "IVI");
    }

    @Test
    @DisplayName("CR-078 TC-VMD-78-04：类别未变（忽略大小写/空白）不触发收敛")
    void apply_shouldNotTriggerConvergenceWhenCategoryUnchanged() {
        VehicleNodeProjectionCommand command = VehicleNodeProjectionCommand.builder()
                .code("TBOX_5G").name("车联终端")
                .deviceCategory(" tbox ").hsmCapability("HSM_FULL")
                .externalRefId("mdm-vn-tbox").externalVersion(8L)
                .build();
        VehicleNode local = VehicleNode.builder()
                .id(1L).code("TBOX_5G").name("车联终端")
                .deviceCategory("TBOX").externalVersion(7L).source(SourceType.MDM)
                .build();
        when(mdmVehicleNodeRepository.selectByCode("TBOX_5G")).thenReturn(local);
        when(mdmVehicleNodeRepository.updateById(any())).thenReturn(1);

        mapper.apply(command);

        verify(partCategoryAutoConvergenceService, never()).onNodeCategoryChanged(any(), any(), any());
    }

    @Test
    @DisplayName("CR-078 TC-VMD-78-07：类别置空不触发收敛（无有效期望值）")
    void apply_shouldNotTriggerConvergenceWhenCategoryCleared() {
        VehicleNodeProjectionCommand command = VehicleNodeProjectionCommand.builder()
                .code("DCU_COCKPIT_SA8295P").name("座舱域控")
                .deviceCategory(null).hsmCapability("HSM_FULL")
                .externalRefId("mdm-vn-dcu").externalVersion(4L)
                .build();
        VehicleNode local = VehicleNode.builder()
                .id(2L).code("DCU_COCKPIT_SA8295P").name("座舱域控")
                .deviceCategory("IVI").externalVersion(3L).source(SourceType.MDM)
                .build();
        when(mdmVehicleNodeRepository.selectByCode("DCU_COCKPIT_SA8295P")).thenReturn(local);
        when(mdmVehicleNodeRepository.updateById(any())).thenReturn(1);

        mapper.apply(command);

        verify(partCategoryAutoConvergenceService, never()).onNodeCategoryChanged(any(), any(), any());
    }

    @Test
    @DisplayName("CR-078：节点首次投影落地也应兜底收敛（零件实例可能早于投影）")
    void apply_shouldTriggerConvergenceOnInsert() {
        VehicleNodeProjectionCommand command = VehicleNodeProjectionCommand.builder()
                .code("DCU_COCKPIT_NEW").name("座舱域控")
                .deviceCategory("IVI").hsmCapability("HSM_FULL")
                .externalRefId("mdm-vn-new").externalVersion(1L)
                .build();
        when(mdmVehicleNodeRepository.selectByCode("DCU_COCKPIT_NEW")).thenReturn(null);
        when(mdmVehicleNodeRepository.insert(any())).thenReturn(1);

        mapper.apply(command);

        verify(partCategoryAutoConvergenceService)
                .onNodeCategoryChanged("DCU_COCKPIT_NEW", null, "IVI");
    }

    @Test
    @DisplayName("CR-078 TC-VMD-78-06：收敛异常不得向上抛出（节点投影已成功）")
    void apply_shouldNotFailWhenConvergenceThrows() {
        VehicleNodeProjectionCommand command = VehicleNodeProjectionCommand.builder()
                .code("DCU_COCKPIT_SA8295P").name("座舱域控")
                .deviceCategory("IVI").hsmCapability("HSM_FULL")
                .externalRefId("mdm-vn-dcu").externalVersion(3L)
                .build();
        VehicleNode local = VehicleNode.builder()
                .id(2L).code("DCU_COCKPIT_SA8295P").name("座舱域控")
                .deviceCategory("DCU").externalVersion(2L).source(SourceType.MDM)
                .build();
        when(mdmVehicleNodeRepository.selectByCode("DCU_COCKPIT_SA8295P")).thenReturn(local);
        when(mdmVehicleNodeRepository.updateById(any())).thenReturn(1);
        doThrow(new IllegalStateException("db down")).when(partCategoryAutoConvergenceService)
                .onNodeCategoryChanged(any(), any(), any());

        assertDoesNotThrow(() -> mapper.apply(command));
        verify(mdmVehicleNodeRepository).updateById(local);
    }
}
