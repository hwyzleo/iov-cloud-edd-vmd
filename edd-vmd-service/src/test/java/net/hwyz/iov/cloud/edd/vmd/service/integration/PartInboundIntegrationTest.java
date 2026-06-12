package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.InboundSourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartInstanceState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartTypeSchema;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartTypeSchemaRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.PartInfoConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartInfoPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 零件入站集成测试
 *
 * @author hwyz_leo
 */
class PartInboundIntegrationTest {

    private static final String MIGRATION_PATH = "src/main/resources/db/migration";

    @Test
    @DisplayName("V21迁移脚本文件应存在")
    void v21MigrationScript_shouldExist() {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V21__Add_inbound_columns_to_part_info.sql");
        assertTrue(Files.exists(migrationFile), "V21迁移脚本文件应该存在");
    }

    @Test
    @DisplayName("V21迁移脚本应包含source字段")
    void v21MigrationScript_shouldContainSourceColumn() throws IOException {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V21__Add_inbound_columns_to_part_info.sql");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("source"), "迁移脚本应包含source字段");
        assertTrue(content.contains("入站来源系统"), "迁移脚本应包含source字段注释");
    }

    @Test
    @DisplayName("V21迁移脚本应包含part_type字段")
    void v21MigrationScript_shouldContainPartTypeColumn() throws IOException {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V21__Add_inbound_columns_to_part_info.sql");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("part_type"), "迁移脚本应包含part_type字段");
        assertTrue(content.contains("零件类型快照"), "迁移脚本应包含part_type字段注释");
    }

    @Test
    @DisplayName("V21迁移脚本应包含inbound_batch_no字段")
    void v21MigrationScript_shouldContainInboundBatchNoColumn() throws IOException {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V21__Add_inbound_columns_to_part_info.sql");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("inbound_batch_no"), "迁移脚本应包含inbound_batch_no字段");
    }

    @Test
    @DisplayName("V21迁移脚本应包含source_event_id字段")
    void v21MigrationScript_shouldContainSourceEventIdColumn() throws IOException {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V21__Add_inbound_columns_to_part_info.sql");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("source_event_id"), "迁移脚本应包含source_event_id字段");
    }

    @Test
    @DisplayName("V21迁移脚本应包含last_inbound_time字段")
    void v21MigrationScript_shouldContainLastInboundTimeColumn() throws IOException {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V21__Add_inbound_columns_to_part_info.sql");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("last_inbound_time"), "迁移脚本应包含last_inbound_time字段");
    }

    @Test
    @DisplayName("V21迁移脚本应修改vehicle_node_code为可空")
    void v21MigrationScript_shouldModifyVehicleNodeCodeNullable() throws IOException {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V21__Add_inbound_columns_to_part_info.sql");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("MODIFY COLUMN"), "迁移脚本应包含MODIFY COLUMN语句");
        assertTrue(content.contains("vehicle_node_code"), "迁移脚本应修改vehicle_node_code");
    }

    @Test
    @DisplayName("V21迁移脚本应创建索引")
    void v21MigrationScript_shouldCreateIndexes() throws IOException {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V21__Add_inbound_columns_to_part_info.sql");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("CREATE INDEX"), "迁移脚本应包含CREATE INDEX语句");
        assertTrue(content.contains("idx_source"), "迁移脚本应创建source索引");
        assertTrue(content.contains("idx_part_type"), "迁移脚本应创建part_type索引");
        assertTrue(content.contains("idx_inbound_batch_no"), "迁移脚本应创建inbound_batch_no索引");
        assertTrue(content.contains("idx_source_event_id"), "迁移脚本应创建source_event_id索引");
    }

    @Test
    @DisplayName("V21迁移脚本应回填历史数据")
    void v21MigrationScript_shouldBackfillHistoricalData() throws IOException {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V21__Add_inbound_columns_to_part_info.sql");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("UPDATE"), "迁移脚本应包含UPDATE语句");
        assertTrue(content.contains("MANUAL"), "迁移脚本应回填MANUAL");
    }

    @Test
    @DisplayName("PartInfoConverter应正确将PO转为领域对象（含入站字段）")
    void testPartInfoConverter_toDomainWithInboundFields() {
        PartInfoPo po = PartInfoPo.builder()
                .id(1L)
                .partCode("PN001")
                .sn("SN001")
                .vehicleNodeCode("TBOX")
                .supplierCode("SUP001")
                .batchNum("BATCH001")
                .instanceState(PartInstanceState.IN_STOCK.value)
                .firstSeenTime(Instant.now())
                .source("MES")
                .partType("TBOX")
                .inboundBatchNo("BATCH001")
                .sourceEventId("EVENT001")
                .lastInboundTime(Instant.now())
                .partName("车载终端")
                .build();

        PartInfo partInfo = PartInfoConverter.INSTANCE.toDomain(po);

        assertNotNull(partInfo);
        assertEquals(1L, partInfo.getId());
        assertEquals("PN001", partInfo.getPartCode());
        assertEquals("SN001", partInfo.getSn());
        assertEquals("TBOX", partInfo.getVehicleNodeCode());
        assertEquals(InboundSourceType.MES, partInfo.getSource());
        assertEquals(PartType.TBOX, partInfo.getPartType());
        assertEquals("BATCH001", partInfo.getInboundBatchNo());
        assertEquals("EVENT001", partInfo.getSourceEventId());
        assertNotNull(partInfo.getLastInboundTime());
        assertEquals("车载终端", partInfo.getPartName());
    }

    @Test
    @DisplayName("PartInfoConverter应正确将领域对象转为PO（含入站字段）")
    void testPartInfoConverter_fromDomainWithInboundFields() {
        PartInfo partInfo = PartInfo.builder()
                .id(1L)
                .partCode("PN001")
                .sn("SN001")
                .vehicleNodeCode("TBOX")
                .supplierCode("SUP001")
                .batchNum("BATCH001")
                .instanceState(PartInstanceState.IN_STOCK.value)
                .firstSeenTime(Instant.now())
                .source(InboundSourceType.MES)
                .partType(PartType.TBOX)
                .inboundBatchNo("BATCH001")
                .sourceEventId("EVENT001")
                .lastInboundTime(Instant.now())
                .partName("车载终端")
                .build();

        PartInfoPo po = PartInfoConverter.INSTANCE.fromDomain(partInfo);

        assertNotNull(po);
        assertEquals(1L, po.getId());
        assertEquals("PN001", po.getPartCode());
        assertEquals("SN001", po.getSn());
        assertEquals("TBOX", po.getVehicleNodeCode());
        assertEquals("MES", po.getSource());
        assertEquals("TBOX", po.getPartType());
        assertEquals("BATCH001", po.getInboundBatchNo());
        assertEquals("EVENT001", po.getSourceEventId());
        assertNotNull(po.getLastInboundTime());
        assertEquals("车载终端", po.getPartName());
    }

    @Test
    @DisplayName("PartInfoConverter应正确处理null枚举值")
    void testPartInfoConverter_nullEnumValues() {
        PartInfoPo po = PartInfoPo.builder()
                .id(1L)
                .partCode("PN001")
                .sn("SN001")
                .source(null)
                .partType(null)
                .partName(null)
                .build();

        PartInfo partInfo = PartInfoConverter.INSTANCE.toDomain(po);

        assertNotNull(partInfo);
        assertNull(partInfo.getSource());
        assertNull(partInfo.getPartType());
        assertNull(partInfo.getPartName());

        PartInfoPo backToPo = PartInfoConverter.INSTANCE.fromDomain(partInfo);

        assertNotNull(backToPo);
        assertNull(backToPo.getSource());
        assertNull(backToPo.getPartType());
        assertNull(backToPo.getPartName());
    }

    @Test
    @DisplayName("PartTypeSchemaRegistry应包含所有默认契约")
    void testPartTypeSchemaRegistry_defaultSchemas() {
        PartTypeSchemaRegistry registry = new PartTypeSchemaRegistry();

        // TBOX
        PartTypeSchema tboxSchema = registry.getSchema(PartType.TBOX);
        assertNotNull(tboxSchema, "TBOX契约应存在");
        assertTrue(tboxSchema.getRequiredFields().contains("sn"), "TBOX应要求sn");
        assertEquals("TBOX", tboxSchema.getDefaultVehicleNodeCode());
        assertEquals("TBOX", tboxSchema.getDefaultDeviceItem());

        // BTM
        PartTypeSchema btmSchema = registry.getSchema(PartType.BTM);
        assertNotNull(btmSchema, "BTM契约应存在");
        assertTrue(btmSchema.getRequiredFields().contains("sn"), "BTM应要求sn");
        assertEquals("BTM_M", btmSchema.getDefaultVehicleNodeCode());
        assertEquals("BTM", btmSchema.getDefaultDeviceItem());

        // CCP
        PartTypeSchema ccpSchema = registry.getSchema(PartType.CCP);
        assertNotNull(ccpSchema, "CCP契约应存在");
        assertEquals("CCP", ccpSchema.getDefaultVehicleNodeCode());

        // IDCM
        PartTypeSchema idcmSchema = registry.getSchema(PartType.IDCM);
        assertNotNull(idcmSchema, "IDCM契约应存在");
        assertEquals("IDCM", idcmSchema.getDefaultVehicleNodeCode());

        // SIM
        PartTypeSchema simSchema = registry.getSchema(PartType.SIM);
        assertNotNull(simSchema, "SIM契约应存在");
        assertTrue(simSchema.getRequiredFields().contains("iccid"), "SIM应要求iccid");
        assertNull(simSchema.getDefaultVehicleNodeCode(), "SIM应无默认vehicleNodeCode");
    }

    @Test
    @DisplayName("PartTypeSchema应正确校验必需字段")
    void testPartTypeSchema_validateRequired() {
        PartTypeSchemaRegistry registry = new PartTypeSchemaRegistry();
        PartTypeSchema tboxSchema = registry.getSchema(PartType.TBOX);

        // 所有字段齐全
        Map<String, String> validFields = new HashMap<>();
        validFields.put("sn", "SN001");
        assertTrue(tboxSchema.validateRequired(validFields).isEmpty());

        // 缺少sn
        Map<String, String> missingSn = new HashMap<>();
        assertEquals(1, tboxSchema.validateRequired(missingSn).size());
        assertTrue(tboxSchema.validateRequired(missingSn).contains("sn"));

        // sn为空
        Map<String, String> blankSn = new HashMap<>();
        blankSn.put("sn", "");
        assertEquals(1, tboxSchema.validateRequired(blankSn).size());
    }

    @Test
    @DisplayName("PartTypeSchema应正确标准化extra字段")
    void testPartTypeSchema_normalizeExtra() {
        PartTypeSchemaRegistry registry = new PartTypeSchemaRegistry();
        PartTypeSchema btmSchema = registry.getSchema(PartType.BTM);

        Map<String, Object> rawFields = new HashMap<>();
        rawFields.put("hsm", "HSM001");
        rawFields.put("mac", "MAC001");
        rawFields.put("emptyField", null);

        String extra = btmSchema.normalizeExtra(rawFields);

        assertNotNull(extra);
        assertTrue(extra.contains("hsm"));
        assertTrue(extra.contains("HSM001"));
        assertTrue(extra.contains("mac"));
        assertTrue(extra.contains("MAC001"));
        assertFalse(extra.contains("emptyField"));
    }

    @Test
    @DisplayName("PartInfo实体应包含所有入站字段")
    void testPartInfoEntity_inboundFields() {
        PartInfo partInfo = PartInfo.builder()
                .id(1L)
                .partCode("PN001")
                .sn("SN001")
                .vehicleNodeCode("TBOX")
                .supplierCode("SUP001")
                .batchNum("BATCH001")
                .instanceState(PartInstanceState.IN_STOCK.value)
                .firstSeenTime(Instant.now())
                .source(InboundSourceType.MES)
                .partType(PartType.TBOX)
                .inboundBatchNo("BATCH001")
                .sourceEventId("EVENT001")
                .lastInboundTime(Instant.now())
                .partName("车载终端")
                .build();

        assertEquals(InboundSourceType.MES, partInfo.getSource());
        assertEquals(PartType.TBOX, partInfo.getPartType());
        assertEquals("BATCH001", partInfo.getInboundBatchNo());
        assertEquals("EVENT001", partInfo.getSourceEventId());
        assertNotNull(partInfo.getLastInboundTime());
        assertEquals("车载终端", partInfo.getPartName());
    }

    @Test
    @DisplayName("InboundSourceType枚举应包含所有预期值")
    void testInboundSourceType_allValues() {
        assertEquals(5, InboundSourceType.values().length);
        assertNotNull(InboundSourceType.valOf("MES"));
        assertNotNull(InboundSourceType.valOf("MANUAL"));
        assertNotNull(InboundSourceType.valOf("WMS"));
        assertNotNull(InboundSourceType.valOf("IQC"));
        assertNotNull(InboundSourceType.valOf("OTHER"));
        assertNull(InboundSourceType.valOf("INVALID"));
    }

    @Test
    @DisplayName("PartType枚举应包含所有预期值")
    void testPartType_allValues() {
        assertEquals(6, PartType.values().length);
        assertNotNull(PartType.valOf("TBOX"));
        assertNotNull(PartType.valOf("BTM"));
        assertNotNull(PartType.valOf("CCP"));
        assertNotNull(PartType.valOf("IDCM"));
        assertNotNull(PartType.valOf("SIM"));
        assertNotNull(PartType.valOf("OTHER"));
        assertNull(PartType.valOf("INVALID"));
    }

    @Test
    @DisplayName("SIM类型契约应要求iccid字段")
    void testSimTypeSchema_requireIccid() {
        PartTypeSchemaRegistry registry = new PartTypeSchemaRegistry();
        PartTypeSchema simSchema = registry.getSchema(PartType.SIM);

        assertNotNull(simSchema);
        assertTrue(simSchema.getRequiredFields().contains("iccid"), "SIM应要求iccid");
        assertFalse(simSchema.getRequiredFields().contains("sn"), "SIM不应要求sn");

        // 验证iccid为空时校验失败
        Map<String, String> emptyFields = new HashMap<>();
        assertFalse(simSchema.validateRequired(emptyFields).isEmpty());

        // 验证iccid有值时校验通过
        Map<String, String> validFields = new HashMap<>();
        validFields.put("iccid", "ICCID001");
        assertTrue(simSchema.validateRequired(validFields).isEmpty());
    }

    @Test
    @DisplayName("PartInstanceState枚举应包含所有预期值")
    void testPartInstanceState_allValues() {
        assertEquals(4, PartInstanceState.values().length);
        assertEquals(0, PartInstanceState.IN_STOCK.value);
        assertEquals(1, PartInstanceState.IN_USE.value);
        assertEquals(2, PartInstanceState.PENDING_REPLACEMENT.value);
        assertEquals(3, PartInstanceState.RETIRED.value);
    }
}
