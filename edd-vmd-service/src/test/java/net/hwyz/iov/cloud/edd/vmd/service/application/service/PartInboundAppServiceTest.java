package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.mdm.api.service.SupplierService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService.PartInboundRecord;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService.PartInboundResult;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartInboundValidateFailedException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.InboundSourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartInboundAppServiceTest {

    @Mock
    private PartInfoAppService partInfoAppService;

    @Mock
    private VehiclePartAppService vehiclePartAppService;

    @Mock
    private MdmPartRepository mdmPartRepository;

    @Mock
    private SupplierService supplierService;

    @Mock
    private PartInfoMutationAppService partInfoMutationAppService;

    private PartInboundAppService partInboundAppService;

    @BeforeEach
    void setUp() {
        partInboundAppService = new PartInboundAppService(
                partInfoAppService, vehiclePartAppService,
                mdmPartRepository, supplierService, partInfoMutationAppService);
    }

    @Test
    @DisplayName("UT-072-02：载荷类别键（三种别名）被剔除，不落入 extra 形成第二权威")
    void processInbound_payloadDeviceCategoryKey_stripped() {
        Map<String, String> extraFields = new HashMap<>();
        extraFields.put("device_category", "DCU");
        extraFields.put("deviceCategory", "TBOX");
        extraFields.put("DEVICE_CATEGORY", "DCU");
        extraFields.put("hsm", "HSM001");

        PartInboundRecord record = PartInboundRecord.builder()
                .partCode("PN001").sn("SN001").partType("BTM")
                .batchNum("BATCH001").extraFields(extraFields).build();

        when(mdmPartRepository.selectByCode("PN001")).thenReturn(Part.builder().status("ACTIVE").partType("BTM").build());
        when(partInfoMutationAppService.create(any(PartInfo.class), any()))
                .thenReturn(new PartInfoMutationAppService.MutationResult("APPLIED", 1, java.util.Map.of()));

        PartInboundResult result = partInboundAppService.processInbound(
                List.of(record), InboundSourceType.MES, null);

        assertEquals(1, result.getSuccessCount());
        verify(partInfoMutationAppService).create(argThat(partInfo -> {
            // 类别键不得进入 extra；普通字段 hsm 保留
            String extra = partInfo.getExtra();
            return extra != null && extra.contains("hsm") && !extra.contains("device_category")
                    && !extra.contains("deviceCategory") && !extra.contains("DEVICE_CATEGORY");
        }), any());
    }

    @Test
    @DisplayName("UT-072-06：节点冲突 → invalid 计数（该项不提交，其他记录继续）")
    void processInbound_nodeConflict_invalidCountContinues() {
        PartInboundRecord conflicting = PartInboundRecord.builder()
                .partCode("PN001").sn("SN001").partType("BTM")
                .vehicleNodeCode("DCU2").batchNum("BATCH001").build();
        PartInboundRecord valid = PartInboundRecord.builder()
                .partCode("PN002").sn("SN002").partType("BTM")
                .batchNum("BATCH001").build();

        when(mdmPartRepository.selectByCode("PN001")).thenReturn(Part.builder().status("ACTIVE").partType("BTM").build());
        when(mdmPartRepository.selectByCode("PN002")).thenReturn(Part.builder().status("ACTIVE").partType("BTM").build());
        when(partInfoMutationAppService.create(any(PartInfo.class), any()))
                .thenThrow(new net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartNodeConflictException(
                        "PN001", "SN001", "DCU1", "DCU2"))
                .thenReturn(new PartInfoMutationAppService.MutationResult("APPLIED", 1, java.util.Map.of()));

        PartInboundResult result = partInboundAppService.processInbound(
                List.of(conflicting, valid), InboundSourceType.MES, null);

        assertEquals(1, result.getInvalidCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
    }

    @Test
    @DisplayName("UT-072-06：类别解析可重试（投影未就绪）→ failure；待治理（数据问题）→ invalid")
    void processInbound_resolveFailure_classified() {
        PartInboundRecord retryable = PartInboundRecord.builder()
                .partCode("PN001").sn("SN001").partType("BTM")
                .vehicleNodeCode("DCU1").batchNum("BATCH001").build();
        PartInboundRecord finalIssue = PartInboundRecord.builder()
                .partCode("PN002").sn("SN002").partType("BTM")
                .vehicleNodeCode("DCU1").batchNum("BATCH001").build();

        when(mdmPartRepository.selectByCode("PN001")).thenReturn(Part.builder().status("ACTIVE").partType("BTM").build());
        when(mdmPartRepository.selectByCode("PN002")).thenReturn(Part.builder().status("ACTIVE").partType("BTM").build());
        when(partInfoMutationAppService.create(any(PartInfo.class), any()))
                .thenThrow(new net.hwyz.iov.cloud.edd.vmd.service.common.exception.DeviceCategoryResolveException(
                        "DCU1", true, "NODE_PROJECTION_NOT_READY_OR_INVALID"))
                .thenThrow(new net.hwyz.iov.cloud.edd.vmd.service.common.exception.DeviceCategoryResolveException(
                        "DCU1", false, "CATEGORY_BLANK"));

        PartInboundResult result = partInboundAppService.processInbound(
                List.of(retryable, finalIssue), InboundSourceType.MES, null);

        assertEquals(1, result.getFailureCount());   // 可重试
        assertEquals(1, result.getInvalidCount());   // 待治理
        assertEquals(0, result.getSuccessCount());
    }

    @Test
    void processInbound_validBtmRecord_success() {
        // Given
        PartInboundRecord record = PartInboundRecord.builder()
                .partCode("PN001")
                .sn("SN001")
                .partType("BTM")
                .supplierCode("SUP001")
                .batchNum("BATCH001")
                .extraFields(Map.of("hsm", "HSM001", "mac", "MAC001"))
                .build();

        when(mdmPartRepository.selectByCode("PN001")).thenReturn(Part.builder().status("ACTIVE").partType("BTM").build());
        when(partInfoMutationAppService.create(any(PartInfo.class), any())).thenReturn(new PartInfoMutationAppService.MutationResult("APPLIED", 1, java.util.Map.of()));

        // When
        PartInboundResult result = partInboundAppService.processInbound(
                List.of(record), InboundSourceType.MES, null);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(0, result.getInvalidCount());
        verify(partInfoMutationAppService).create(any(PartInfo.class), any());
    }

    @Test
    void processInbound_validSimRecord_success() {
        // Given
        Map<String, String> extraFields = new HashMap<>();
        extraFields.put("iccid", "ICCID001");
        extraFields.put("imsi", "IMSI001");
        extraFields.put("msisdn", "MSISDN001");
        extraFields.put("mno", "CMCC");

        PartInboundRecord record = PartInboundRecord.builder()
                .partCode("SIM001")
                .sn("ICCID001")
                .partType("SIM")
                .batchNum("BATCH001")
                .extraFields(extraFields)
                .build();

        when(mdmPartRepository.selectByCode("SIM001")).thenReturn(Part.builder().status("ACTIVE").partType("SIM").build());
        when(partInfoMutationAppService.create(any(PartInfo.class), any())).thenReturn(new PartInfoMutationAppService.MutationResult("APPLIED", 1, java.util.Map.of()));

        // When
        PartInboundResult result = partInboundAppService.processInbound(
                List.of(record), InboundSourceType.MES, null);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        verify(partInfoMutationAppService).create(argThat(partInfo ->
                "ICCID001".equals(partInfo.getSn()) &&
                "SIM".equals(partInfo.getPartType())), any());
    }

    @Test
    void processInbound_blankPartCode_invalidCount() {
        // Given
        PartInboundRecord record = PartInboundRecord.builder()
                .partCode("")
                .sn("SN001")
                .partType("BTM")
                .build();

        // When
        PartInboundResult result = partInboundAppService.processInbound(
                List.of(record), InboundSourceType.MES, null);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(1, result.getInvalidCount());
        verify(partInfoMutationAppService, never()).create(any(), any());
    }

    @Test
    void processInbound_withVin_bindsVehiclePart() {
        // Given
        PartInboundRecord record = PartInboundRecord.builder()
                .partCode("PN001")
                .sn("SN001")
                .partType("TBOX")
                .supplierCode("SUP001")
                .batchNum("BATCH001")
                .build();

        when(mdmPartRepository.selectByCode("PN001")).thenReturn(Part.builder().status("ACTIVE").partType("TBOX").build());
        when(partInfoMutationAppService.create(any(PartInfo.class), any())).thenAnswer(invocation -> {
            PartInfo partInfo = invocation.getArgument(0);
            partInfo.setId(1L);
            return new PartInfoMutationAppService.MutationResult("APPLIED", 1, java.util.Map.of());
        });

        // When
        PartInboundResult result = partInboundAppService.processInbound(
                List.of(record), InboundSourceType.MES, "VIN001");

        // Then
        assertEquals(1, result.getSuccessCount());
        verify(vehiclePartAppService).bindVehiclePart(any(VehiclePart.class));
    }

    @Test
    void processInbound_multipleRecords_partialSuccess() {
        // Given
        PartInboundRecord validRecord = PartInboundRecord.builder()
                .partCode("PN001")
                .sn("SN001")
                .partType("BTM")
                .batchNum("BATCH001")
                .build();

        PartInboundRecord invalidRecord = PartInboundRecord.builder()
                .partCode("")
                .sn("SN002")
                .partType("BTM")
                .batchNum("BATCH001")
                .build();

        when(mdmPartRepository.selectByCode("PN001")).thenReturn(Part.builder().status("ACTIVE").partType("BTM").build());
        when(partInfoMutationAppService.create(any(PartInfo.class), any())).thenReturn(new PartInfoMutationAppService.MutationResult("APPLIED", 1, java.util.Map.of()));

        // When
        PartInboundResult result = partInboundAppService.processInbound(
                List.of(validRecord, invalidRecord), InboundSourceType.MES, null);

        // Then
        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(1, result.getInvalidCount());
    }

    @Test
    @DisplayName("Should validate part type against MDM Part projection")
    void shouldValidatePartTypeAgainstMdmPartProjection() {
        // Given
        PartInboundRecord record = PartInboundRecord.builder()
                .partCode("TEST_PART_001")
                .sn("TEST_SN_001")
                .partType("TBOX")
                .build();

        Part mdmPart = Part.builder()
                .code("TEST_PART_001")
                .name("Test Part")
                .partType("TBOX")
                .status("ACTIVE")
                .build();

        when(mdmPartRepository.selectByCode("TEST_PART_001")).thenReturn(mdmPart);
        when(partInfoMutationAppService.create(any(PartInfo.class), any())).thenAnswer(invocation -> {
            PartInfo partInfo = invocation.getArgument(0);
            partInfo.setId(1L);
            return new PartInfoMutationAppService.MutationResult("APPLIED", 1, java.util.Map.of());
        });

        // When
        PartInfo result = partInboundAppService.processSingleInbound(
                record, InboundSourceType.MES, null);

        // Then
        assertNotNull(result);
        assertEquals("TBOX", result.getPartType());
        verify(mdmPartRepository).selectByCode("TEST_PART_001");
    }

    @Test
    void processSingleInbound_validRecord_success() {
        // Given
        PartInboundRecord record = PartInboundRecord.builder()
                .partCode("PN001")
                .sn("SN001")
                .partType("TBOX")
                .supplierCode("SUP001")
                .batchNum("BATCH001")
                .build();

        when(mdmPartRepository.selectByCode("PN001")).thenReturn(Part.builder().status("ACTIVE").partType("TBOX").build());
        when(partInfoMutationAppService.create(any(PartInfo.class), any())).thenAnswer(invocation -> {
            PartInfo partInfo = invocation.getArgument(0);
            partInfo.setId(1L);
            return new PartInfoMutationAppService.MutationResult("APPLIED", 1, java.util.Map.of());
        });

        // When
        PartInfo result = partInboundAppService.processSingleInbound(
                record, InboundSourceType.MES, "VIN001");

        // Then
        assertNotNull(result);
        assertEquals("PN001", result.getPartCode());
        assertEquals("SN001", result.getSn());
        assertEquals("TBOX", result.getPartType());
        assertEquals(InboundSourceType.MES, result.getSource());
        verify(vehiclePartAppService).bindVehiclePart(any(VehiclePart.class));
    }
}
