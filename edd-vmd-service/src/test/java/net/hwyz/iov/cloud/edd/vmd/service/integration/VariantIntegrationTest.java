package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptVariantAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.VariantRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VariantResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VariantAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VariantCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VariantDto;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ProductDataReadOnlyException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Variant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VariantConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmVariantPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class VariantIntegrationTest {

    @Test
    @DisplayName("Variant领域对象应正确构建")
    void testVariantEntityBuild() {
        Variant variant = Variant.builder()
                .id(1L)
                .modelCode("M001")
                .code("V001")
                .name("测试版本")
                .nameLocal("测试版本本地化")
                .description("版本备注")
                .source(SourceType.MANUAL)
                .build();

        assertEquals(1L, variant.getId());
        assertEquals("M001", variant.getModelCode());
        assertEquals("V001", variant.getCode());
        assertEquals("测试版本", variant.getName());
        assertEquals("测试版本本地化", variant.getNameLocal());
        assertEquals("版本备注", variant.getDescription());
        assertEquals(SourceType.MANUAL, variant.getSource());
    }

    @Test
    @DisplayName("VariantAssembler应正确将领域对象转为DTO")
    void testVariantAssembler_fromDomain() {
        Variant variant = Variant.builder()
                .id(1L)
                .modelCode("M001")
                .code("V001")
                .name("测试版本")
                .nameLocal("测试版本本地化")
                .description("版本备注")
                .build();

        VariantDto dto = VariantAssembler.INSTANCE.fromDomain(variant);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("M001", dto.getModelCode());
        assertEquals("V001", dto.getCode());
        assertEquals("测试版本", dto.getName());
        assertEquals("测试版本本地化", dto.getNameLocal());
        assertEquals("版本备注", dto.getDescription());
    }

    @Test
    @DisplayName("VariantAssembler应正确将Cmd转为领域对象")
    void testVariantAssembler_toDomain() {
        VariantCmd cmd = VariantCmd.builder()
                .id(1L)
                .modelCode("M001")
                .code("V001")
                .name("测试版本")
                .nameLocal("测试版本本地化")
                .description("版本备注")
                .build();

        Variant variant = VariantAssembler.INSTANCE.toDomain(cmd);

        assertNotNull(variant);
        assertEquals(1L, variant.getId());
        assertEquals("M001", variant.getModelCode());
        assertEquals("V001", variant.getCode());
        assertEquals("测试版本", variant.getName());
        assertEquals("测试版本本地化", variant.getNameLocal());
        assertEquals("版本备注", variant.getDescription());
    }

    @Test
    @DisplayName("MptVariantAssembler应正确将DTO转为Response（派生平台/车系保留，CR-048）")
    void testMptVariantAssembler_fromDto() {
        VariantDto dto = VariantDto.builder()
                .id(1L)
                .platformCode("P001")
                .carLineCode("CL001")
                .modelCode("M001")
                .code("V001")
                .name("测试版本")
                .nameLocal("测试版本本地化")
                .description("版本备注")
                .build();

        VariantResponse response = MptVariantAssembler.INSTANCE.fromDto(dto);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("P001", response.getPlatformCode());
        assertEquals("CL001", response.getCarLineCode());
        assertEquals("M001", response.getModelCode());
        assertEquals("V001", response.getCode());
        assertEquals("测试版本", response.getName());
        assertEquals("测试版本本地化", response.getNameLocal());
    }

    @Test
    @DisplayName("MptVariantAssembler应正确将Request转为Cmd")
    void testMptVariantAssembler_toCmd() {
        VariantRequest request = VariantRequest.builder()
                .id(1L)
                .modelCode("M001")
                .code("V001")
                .name("测试版本")
                .nameLocal("测试版本本地化")
                .description("版本备注")
                .build();

        VariantCmd cmd = MptVariantAssembler.INSTANCE.toCmd(request);

        assertNotNull(cmd);
        assertEquals(1L, cmd.getId());
        assertEquals("M001", cmd.getModelCode());
        assertEquals("V001", cmd.getCode());
        assertEquals("测试版本", cmd.getName());
        assertEquals("测试版本本地化", cmd.getNameLocal());
        assertEquals("版本备注", cmd.getDescription());
    }

    @Test
    @DisplayName("VariantConverter应正确将PO转为领域对象")
    void testVariantConverter_toDomain() {
        MdmVariantPo po = MdmVariantPo.builder()
                .id(1L)
                .modelCode("M001")
                .code("V001")
                .name("测试版本")
                .nameLocal("测试版本本地化")
                .source("MDM")
                .externalRefId("ext-001")
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();

        Variant variant = VariantConverter.INSTANCE.toDomain(po);

        assertNotNull(variant);
        assertEquals(1L, variant.getId());
        assertEquals("M001", variant.getModelCode());
        assertEquals("V001", variant.getCode());
        assertEquals(SourceType.MDM, variant.getSource());
        assertEquals("ext-001", variant.getExternalRefId());
        assertEquals(1L, variant.getExternalVersion());
        assertNotNull(variant.getLastSyncTime());
    }

    @Test
    @DisplayName("VariantConverter应正确将领域对象转为PO")
    void testVariantConverter_fromDomain() {
        Variant variant = Variant.builder()
                .id(1L)
                .modelCode("M001")
                .code("V001")
                .name("测试版本")
                .nameLocal("测试版本本地化")
                .source(SourceType.MDM)
                .externalRefId("ext-001")
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();

        MdmVariantPo po = VariantConverter.INSTANCE.fromDomain(variant);

        assertNotNull(po);
        assertEquals(1L, po.getId());
        assertEquals("M001", po.getModelCode());
        assertEquals("V001", po.getCode());
        assertEquals("MDM", po.getSource());
        assertEquals("ext-001", po.getExternalRefId());
        assertEquals(1L, po.getExternalVersion());
        assertNotNull(po.getLastSyncTime());
    }

    @Test
    @DisplayName("ProductDataReadOnlyException应包含正确的错误信息")
    void testProductDataReadOnlyException() {
        ProductDataReadOnlyException ex = new ProductDataReadOnlyException("版本", "V001");

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("版本"));
        assertTrue(ex.getMessage().contains("V001"));
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
