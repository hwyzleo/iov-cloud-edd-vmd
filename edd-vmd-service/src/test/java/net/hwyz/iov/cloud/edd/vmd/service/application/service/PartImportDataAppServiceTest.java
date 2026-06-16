package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.json.JSONObject;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessorRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.ProduceDataParserV1_0;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
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
class PartImportDataAppServiceTest {

    @Mock
    private ImportDataParserRegistry parserRegistry;

    @Mock
    private PartImportDataRepository partImportDataRepository;

    @Mock
    private MdmPartRepository mdmPartRepository;

    @Mock
    private ProduceDataParserV1_0 produceDataParserV1_0Bean;

    @Mock
    private DownstreamProcessorRegistry downstreamProcessorRegistry;

    private PartImportDataAppService partImportDataAppService;

    @BeforeEach
    void setUp() {
        partImportDataAppService = new PartImportDataAppService(
                parserRegistry, partImportDataRepository, mdmPartRepository, produceDataParserV1_0Bean, downstreamProcessorRegistry);
    }

    @Test
    @DisplayName("Should separate PRODUCE type from other part types")
    void shouldSeparateProduceTypeFromOtherPartTypes() {
        // Given
        String batchNum = "TEST_BATCH_001";
        PartImportData importData = PartImportData.builder()
                .batchNum(batchNum)
                .partCode("PRODUCE")
                .version("1.0")
                .data("{\"VIN\":\"TEST_VIN\",\"PARTS\":[]}")
                .handle(false)
                .build();

        when(partImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(produceDataParserV1_0Bean.parse(eq(batchNum), any(JSONObject.class)))
                .thenReturn(ImportResult.builder().build());

        // When
        partImportDataAppService.parsePartImportData(batchNum);

        // Then
        verify(produceDataParserV1_0Bean).parse(eq(batchNum), any(JSONObject.class));
        verify(parserRegistry, never()).getParser(anyString(), anyString());
    }
}
