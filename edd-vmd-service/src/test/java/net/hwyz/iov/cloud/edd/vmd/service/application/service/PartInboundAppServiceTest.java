package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService.PartInboundRecord;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService.PartInboundResult;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartInboundValidateFailedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartTypeSchemaNotFoundException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.InboundSourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartTypeSchemaRegistry;
import org.junit.jupiter.api.BeforeEach;
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

    private PartTypeSchemaRegistry partTypeSchemaRegistry;

    private PartInboundAppService partInboundAppService;

    @BeforeEach
    void setUp() {
        partTypeSchemaRegistry = new PartTypeSchemaRegistry();
        partInboundAppService = new PartInboundAppService(
                partInfoAppService, vehiclePartAppService, partTypeSchemaRegistry);
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

        when(partInfoAppService.upsertPartInfo(any(PartInfo.class))).thenReturn(1);

        // When
        PartInboundResult result = partInboundAppService.processInbound(
                List.of(record), InboundSourceType.MES, null);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        verify(partInfoAppService).upsertPartInfo(argThat(partInfo ->
                "ICCID001".equals(partInfo.getSn()) &&
                PartType.SIM.equals(partInfo.getPartType())));
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
    void processInbound_invalidPartType_failureCount() {
        // Given
        PartInboundRecord record = PartInboundRecord.builder()
                .partCode("PN001")
                .sn("SN001")
                .partType("INVALID_TYPE")
                .build();

        // When
        PartInboundResult result = partInboundAppService.processInbound(
                List.of(record), InboundSourceType.MES, null);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(0, result.getInvalidCount());
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
    void processSingleInbound_validRecord_success() {
        // Given
        PartInboundRecord record = PartInboundRecord.builder()
                .partCode("PN001")
                .sn("SN001")
                .partType("TBOX")
                .supplierCode("SUP001")
                .batchNum("BATCH001")
                .build();

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
        assertEquals(PartType.TBOX, result.getPartType());
        assertEquals(InboundSourceType.MES, result.getSource());
        verify(vehiclePartAppService).bindVehiclePart(any(VehiclePart.class));
    }
}
