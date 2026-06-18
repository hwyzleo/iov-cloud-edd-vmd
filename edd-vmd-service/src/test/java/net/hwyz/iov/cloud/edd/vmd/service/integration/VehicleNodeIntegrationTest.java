package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptVehicleNodeAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.VehicleNodeRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VehicleNodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehicleNodeAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleNodeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleNodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ProductDataReadOnlyException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehicleNodeConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmVehicleNodePo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class VehicleNodeIntegrationTest {

    @Test
    @DisplayName("VehicleNode领域对象应正确构建")
    void testVehicleNodeEntityBuild() {
        VehicleNode vehicleNode = VehicleNode.builder()
                .id(1L)
                .code("VN001")
                .name("测试车载节点")
                .nameLocal("Test Vehicle Node")
                .deviceCategory("TBOX")
                .funcDomain("ADAS")
                .nodeType("CONTROLLER")
                .otaSupport("OTA")
                .core(true)
                .sort(1)
                .source(SourceType.MANUAL)
                .build();

        assertEquals(1L, vehicleNode.getId());
        assertEquals("VN001", vehicleNode.getCode());
        assertEquals("测试车载节点", vehicleNode.getName());
        assertEquals("Test Vehicle Node", vehicleNode.getNameLocal());
        assertEquals("TBOX", vehicleNode.getDeviceCategory());
        assertEquals("ADAS", vehicleNode.getFuncDomain());
        assertEquals("CONTROLLER", vehicleNode.getNodeType());
        assertEquals("OTA", vehicleNode.getOtaSupport());
        assertTrue(vehicleNode.getCore());
        assertEquals(1, vehicleNode.getSort());
        assertEquals(SourceType.MANUAL, vehicleNode.getSource());
    }

    @Test
    @DisplayName("VehicleNodeAssembler应正确将领域对象转为DTO")
    void testVehicleNodeAssembler_fromDomain() {
        VehicleNode vehicleNode = VehicleNode.builder()
                .id(1L)
                .code("VN001")
                .name("测试车载节点")
                .nameLocal("Test Vehicle Node")
                .deviceCategory("TBOX")
                .funcDomain("ADAS")
                .nodeType("CONTROLLER")
                .otaSupport("OTA")
                .core(true)
                .sort(1)
                .build();

        VehicleNodeDto dto = VehicleNodeAssembler.INSTANCE.fromDomain(vehicleNode);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("VN001", dto.getCode());
        assertEquals("测试车载节点", dto.getName());
        assertEquals("Test Vehicle Node", dto.getNameLocal());
        assertEquals("TBOX", dto.getDeviceCategory());
        assertEquals("ADAS", dto.getFuncDomain());
        assertNotNull(dto.getNodeType());
        assertEquals("CONTROLLER", dto.getNodeType());
        assertEquals("OTA", dto.getOtaSupport());
        assertTrue(dto.getCore());
        assertEquals(1, dto.getSort());
    }

    @Test
    @DisplayName("VehicleNodeAssembler应正确将Cmd转为领域对象")
    void testVehicleNodeAssembler_toDomain() {
        VehicleNodeCmd cmd = VehicleNodeCmd.builder()
                .id(1L)
                .code("VN001")
                .name("测试车载节点")
                .nameLocal("Test Vehicle Node")
                .deviceCategory("TBOX")
                .funcDomain("ADAS")
                .nodeType("CONTROLLER")
                .otaSupport("OTA")
                .core(true)
                .sort(1)
                .build();

        VehicleNode vehicleNode = VehicleNodeAssembler.INSTANCE.toDomain(cmd);

        assertNotNull(vehicleNode);
        assertEquals(1L, vehicleNode.getId());
        assertEquals("VN001", vehicleNode.getCode());
        assertEquals("测试车载节点", vehicleNode.getName());
        assertEquals("Test Vehicle Node", vehicleNode.getNameLocal());
        assertEquals("TBOX", vehicleNode.getDeviceCategory());
        assertEquals("ADAS", vehicleNode.getFuncDomain());
        assertNotNull(vehicleNode.getNodeType());
        assertEquals("CONTROLLER", vehicleNode.getNodeType());
        assertEquals("OTA", vehicleNode.getOtaSupport());
        assertTrue(vehicleNode.getCore());
        assertEquals(1, vehicleNode.getSort());
    }

    @Test
    @DisplayName("MptVehicleNodeAssembler应正确将DTO转为Response")
    void testMptVehicleNodeAssembler_fromDto() {
        VehicleNodeDto dto = VehicleNodeDto.builder()
                .id(1L)
                .code("VN001")
                .name("测试车载节点")
                .nameLocal("Test Vehicle Node")
                .deviceCategory("TBOX")
                .funcDomain("ADAS")
                .nodeType("CONTROLLER")
                .otaSupport("OTA")
                .core(true)
                .sort(1)
                .build();

        VehicleNodeResponse response = MptVehicleNodeAssembler.INSTANCE.fromDto(dto);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("VN001", response.getCode());
        assertEquals("测试车载节点", response.getName());
        assertEquals("Test Vehicle Node", response.getNameLocal());
        assertEquals("TBOX", response.getDeviceCategory());
        assertEquals("ADAS", response.getFuncDomain());
        assertNotNull(response.getNodeType());
        assertEquals("CONTROLLER", response.getNodeType());
        assertEquals("OTA", response.getOtaSupport());
        assertTrue(response.getCore());
        assertEquals(1, response.getSort());
    }

    @Test
    @DisplayName("MptVehicleNodeAssembler应正确将Request转为Cmd")
    void testMptVehicleNodeAssembler_toCmd() {
        VehicleNodeRequest request = VehicleNodeRequest.builder()
                .id(1L)
                .code("VN001")
                .name("测试车载节点")
                .nameLocal("Test Vehicle Node")
                .deviceCategory("TBOX")
                .funcDomain("ADAS")
                .nodeType("CONTROLLER")
                .otaSupport("OTA")
                .core(true)
                .sort(1)
                .build();

        VehicleNodeCmd cmd = MptVehicleNodeAssembler.INSTANCE.toCmd(request);

        assertNotNull(cmd);
        assertEquals(1L, cmd.getId());
        assertEquals("VN001", cmd.getCode());
        assertEquals("测试车载节点", cmd.getName());
        assertEquals("Test Vehicle Node", cmd.getNameLocal());
        assertEquals("TBOX", cmd.getDeviceCategory());
        assertEquals("ADAS", cmd.getFuncDomain());
        assertNotNull(cmd.getNodeType());
        assertEquals("CONTROLLER", cmd.getNodeType());
        assertEquals("OTA", cmd.getOtaSupport());
        assertTrue(cmd.getCore());
        assertEquals(1, cmd.getSort());
    }

    @Test
    @DisplayName("VehicleNodeConverter应正确将PO转为领域对象")
    void testVehicleNodeConverter_toDomain() {
        MdmVehicleNodePo po = MdmVehicleNodePo.builder()
                .id(1L)
                .code("VN001")
                .name("测试车载节点")
                .nameLocal("Test Vehicle Node")
                .deviceCategory("TBOX")
                .funcDomain("ADAS")
                .nodeType("CONTROLLER")
                .otaSupport("OTA")
                .core(true)
                .sort(1)
                .source("MDM")
                .externalRefId("ext-001")
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();

        VehicleNode vehicleNode = VehicleNodeConverter.INSTANCE.toDomain(po);

        assertNotNull(vehicleNode);
        assertEquals(1L, vehicleNode.getId());
        assertEquals("VN001", vehicleNode.getCode());
        assertEquals("测试车载节点", vehicleNode.getName());
        assertEquals("TBOX", vehicleNode.getDeviceCategory());
        assertEquals("ADAS", vehicleNode.getFuncDomain());
        assertEquals(SourceType.MDM, vehicleNode.getSource());
        assertEquals("ext-001", vehicleNode.getExternalRefId());
        assertEquals(1L, vehicleNode.getExternalVersion());
        assertNotNull(vehicleNode.getLastSyncTime());
    }

    @Test
    @DisplayName("VehicleNodeConverter应正确将领域对象转为PO")
    void testVehicleNodeConverter_fromDomain() {
        VehicleNode vehicleNode = VehicleNode.builder()
                .id(1L)
                .code("VN001")
                .name("测试车载节点")
                .nameLocal("Test Vehicle Node")
                .deviceCategory("TBOX")
                .funcDomain("ADAS")
                .nodeType("CONTROLLER")
                .otaSupport("OTA")
                .core(true)
                .sort(1)
                .source(SourceType.MDM)
                .externalRefId("ext-001")
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();

        MdmVehicleNodePo po = VehicleNodeConverter.INSTANCE.fromDomain(vehicleNode);

        assertNotNull(po);
        assertEquals(1L, po.getId());
        assertEquals("VN001", po.getCode());
        assertEquals("测试车载节点", po.getName());
        assertEquals("TBOX", po.getDeviceCategory());
        assertEquals("ADAS", po.getFuncDomain());
        assertEquals("MDM", po.getSource());
        assertEquals("ext-001", po.getExternalRefId());
        assertEquals(1L, po.getExternalVersion());
        assertNotNull(po.getLastSyncTime());
    }

    @Test
    @DisplayName("ProductDataReadOnlyException应包含正确的错误信息")
    void testProductDataReadOnlyException() {
        ProductDataReadOnlyException ex = new ProductDataReadOnlyException("车载节点", "VN001");

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("车载节点"));
        assertTrue(ex.getMessage().contains("VN001"));
        assertTrue(ex.getMessage().contains("MDM"));
    }

    @Test
    @DisplayName("SourceType枚举应正确解析值")
    void testSourceType() {
        assertEquals(SourceType.MDM, SourceType.valOf("MDM"));
        assertEquals(SourceType.MANUAL, SourceType.valOf("MANUAL"));
        assertNull(SourceType.valOf("UNKNOWN"));

        assertEquals("MDM", SourceType.MDM.getValue());
        assertEquals("MANUAL", SourceType.MANUAL.getValue());
    }
}
