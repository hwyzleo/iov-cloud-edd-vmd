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

    private PartInboundAppService partInboundAppService;

    @BeforeEach
    void setUp() {
        partInboundAppService = new PartInboundAppService(
                partInfoAppService, vehiclePartAppService,
                mdmPartRepository, supplierService);
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
        when(partInfoAppService.upsertPartInfo(any(PartInfo.class))).thenReturn(1);

        // When
        PartInboundResult result = partInboundAppService.processInbound(
                List.of(record), InboundSourceType.MES, null);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(0, result.getInvalidCount());
        verify(partInfoAppService).upsertPartInfo(any(PartInfo.class));
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
        when(partInfoAppService.upsertPartInfo(any(PartInfo.class))).thenReturn(1);

        // When
        PartInboundResult result = partInboundAppService.processInbound(
                List.of(record), InboundSourceType.MES, null);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        verify(partInfoAppService).upsertPartInfo(argThat(partInfo ->
                "ICCID001".equals(partInfo.getSn()) &&
                "SIM".equals(partInfo.getPartType())));
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
        verify(partInfoAppService, never()).upsertPartInfo(any());
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
        when(partInfoAppService.upsertPartInfo(any(PartInfo.class))).thenAnswer(invocation -> {
            PartInfo partInfo = invocation.getArgument(0);
            partInfo.setId(1L);
            return 1;
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
        when(partInfoAppService.upsertPartInfo(any(PartInfo.class))).thenReturn(1);

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
        when(partInfoAppService.upsertPartInfo(any(PartInfo.class))).thenAnswer(invocation -> {
            PartInfo partInfo = invocation.getArgument(0);
            partInfo.setId(1L);
            return 1;
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
        when(partInfoAppService.upsertPartInfo(any(PartInfo.class))).thenAnswer(invocation -> {
            PartInfo partInfo = invocation.getArgument(0);
            partInfo.setId(1L);
            return 1;
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
