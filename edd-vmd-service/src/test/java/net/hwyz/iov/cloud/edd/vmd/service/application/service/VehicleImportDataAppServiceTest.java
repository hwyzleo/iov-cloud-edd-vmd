package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.ProduceDataParserV1_0;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleImportDataAppServiceTest {

    @Mock
    private ImportDataParserRegistry parserRegistry;

    @Mock
    private VehImportDataRepository vehImportDataRepository;

    @Mock
    private ImportDataParser produceDataParserV1_0;

    @Mock
    private ImportDataParser vehicleProduceDataParserV1_0;

    @Mock
    private ProduceDataParserV1_0 produceDataParserV1_0Bean;

    private VehicleImportDataAppService vehicleImportDataAppService;

    @BeforeEach
    void setUp() {
        vehicleImportDataAppService = new VehicleImportDataAppService(
                parserRegistry, vehImportDataRepository, produceDataParserV1_0Bean);
    }

    @Test
    @DisplayName("Should separate PRODUCE type from other part types")
    void shouldSeparateProduceTypeFromOtherPartTypes() {
        // Given
        String batchNum = "TEST_BATCH_001";
        VehicleImportData importData = VehicleImportData.builder()
                .batchNum(batchNum)
                .type("PRODUCE")
                .version("1.0")
                .data("{\"VIN\":\"TEST_VIN\",\"PARTS\":[]}")
                .handle(false)
                .build();

        when(vehImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(produceDataParserV1_0Bean.parse(eq(batchNum), any(JSONObject.class)))
                .thenReturn(ImportResult.builder().build());

        // When
        vehicleImportDataAppService.parseVehicleImportData(batchNum);

        // Then
        verify(produceDataParserV1_0Bean).parse(eq(batchNum), any(JSONObject.class));
        verify(parserRegistry, never()).getParser(anyString(), anyString());
    }
}
