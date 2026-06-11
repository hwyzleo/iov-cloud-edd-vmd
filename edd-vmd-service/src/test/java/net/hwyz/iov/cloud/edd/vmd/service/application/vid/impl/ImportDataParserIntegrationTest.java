package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService.PartInboundResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.InboundSourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartTypeSchemaRegistry;
import net.hwyz.iov.cloud.iov.idk.api.service.IdkBtmInfoService;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspCcpInfoService;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspIdcmInfoService;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspSimService;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspTboxInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 导入数据解析器集成测试
 * <p>
 * 验证解析器正确注册、获取和调用入站内核
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class ImportDataParserIntegrationTest {

    @Mock
    private PartInboundAppService partInboundAppService;

    @Mock
    private IdkBtmInfoService idkBtmInfoService;

    @Mock
    private TspTboxInfoService tspTboxInfoService;

    @Mock
    private TspCcpInfoService tspCcpInfoService;

    @Mock
    private TspIdcmInfoService tspIdcmInfoService;

    @Mock
    private TspSimService tspSimService;

    private ImportDataParserRegistry parserRegistry;
    private PartTypeSchemaRegistry partTypeSchemaRegistry;

    @BeforeEach
    void setUp() {
        parserRegistry = new ImportDataParserRegistry();
        partTypeSchemaRegistry = new PartTypeSchemaRegistry();
    }

    @Test
    @DisplayName("解析器应正确注册到注册表")
    void parser_shouldRegisterCorrectly() {
        // Given
        BtmDataParserV1_0 btmParser = createBtmParser();
        TboxDataParserV1_0 tboxParser = createTboxParser();
        CcpDataParserV1_0 ccpParser = createCcpParser();
        IdcmDataParserV1_0 idcmParser = createIdcmParser();
        SimDataParserV1_0 simParser = createSimParser();

        // When - 解析器在@PostConstruct中自注册

        // Then
        assertNotNull(parserRegistry.getParser("BTM", "1.0"));
        assertNotNull(parserRegistry.getParser("TBOX", "1.0"));
        assertNotNull(parserRegistry.getParser("CCP", "1.0"));
        assertNotNull(parserRegistry.getParser("IDCM", "1.0"));
        assertNotNull(parserRegistry.getParser("SIM", "1.0"));
    }

    @Test
    @DisplayName("获取不存在的解析器应抛出异常")
    void getParser_notExist_shouldThrowException() {
        // When & Then
        assertThrows(Exception.class, () -> parserRegistry.getParser("UNKNOWN", "1.0"));
    }

    @Test
    @DisplayName("BTM解析器应调用入站内核")
    void btmParser_shouldCallInboundKernel() {
        // Given
        BtmDataParserV1_0 btmParser = createBtmParser();

        JSONObject dataJson = buildBtmDataJson("SUP001", 
                new String[][]{{"PN001", "SN001", "HSM001", "MAC001"}}
        );

        PartInboundResult expectedResult = PartInboundResult.builder()
                .totalCount(1)
                .successCount(1)
                .failureCount(0)
                .invalidCount(0)
                .build();

        when(partInboundAppService.processInbound(any(), eq(InboundSourceType.MES), any()))
                .thenReturn(expectedResult);

        // When
        ImportResult result = btmParser.parse("BATCH001", dataJson);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        verify(partInboundAppService).processInbound(any(), eq(InboundSourceType.MES), any());
    }

    @Test
    @DisplayName("TBOX解析器应调用入站内核")
    void tboxParser_shouldCallInboundKernel() {
        // Given
        TboxDataParserV1_0 tboxParser = createTboxParser();

        JSONObject dataJson = buildTboxDataJson("SUP001", 
                new String[][]{{"PN001", "SN001", "ICCID1", "ICCID2", "IMEI001", "HSM001"}}
        );

        PartInboundResult expectedResult = PartInboundResult.builder()
                .totalCount(1)
                .successCount(1)
                .failureCount(0)
                .invalidCount(0)
                .build();

        when(partInboundAppService.processInbound(any(), eq(InboundSourceType.MES), any()))
                .thenReturn(expectedResult);

        // When
        ImportResult result = tboxParser.parse("BATCH001", dataJson);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        verify(partInboundAppService).processInbound(any(), eq(InboundSourceType.MES), any());
    }

    @Test
    @DisplayName("CCP解析器应调用入站内核")
    void ccpParser_shouldCallInboundKernel() {
        // Given
        CcpDataParserV1_0 ccpParser = createCcpParser();

        JSONObject dataJson = buildCcpDataJson("SUP001", 
                new String[][]{{"PN001", "SN001", "HSM001"}}
        );

        PartInboundResult expectedResult = PartInboundResult.builder()
                .totalCount(1)
                .successCount(1)
                .failureCount(0)
                .invalidCount(0)
                .build();

        when(partInboundAppService.processInbound(any(), eq(InboundSourceType.MES), any()))
                .thenReturn(expectedResult);

        // When
        ImportResult result = ccpParser.parse("BATCH001", dataJson);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        verify(partInboundAppService).processInbound(any(), eq(InboundSourceType.MES), any());
    }

    @Test
    @DisplayName("IDCM解析器应调用入站内核")
    void idcmParser_shouldCallInboundKernel() {
        // Given
        IdcmDataParserV1_0 idcmParser = createIdcmParser();

        JSONObject dataJson = buildIdcmDataJson("SUP001", 
                new String[][]{{"PN001", "SN001", "HSM001", "MAC001"}}
        );

        PartInboundResult expectedResult = PartInboundResult.builder()
                .totalCount(1)
                .successCount(1)
                .failureCount(0)
                .invalidCount(0)
                .build();

        when(partInboundAppService.processInbound(any(), eq(InboundSourceType.MES), any()))
                .thenReturn(expectedResult);

        // When
        ImportResult result = idcmParser.parse("BATCH001", dataJson);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        verify(partInboundAppService).processInbound(any(), eq(InboundSourceType.MES), any());
    }

    @Test
    @DisplayName("SIM解析器应调用入站内核")
    void simParser_shouldCallInboundKernel() {
        // Given
        SimDataParserV1_0 simParser = createSimParser();

        JSONObject dataJson = buildSimDataJson("CMCC", 
                new String[][]{{"ICCID001", "IMSI001", "MSISDN001"}}
        );

        PartInboundResult expectedResult = PartInboundResult.builder()
                .totalCount(1)
                .successCount(1)
                .failureCount(0)
                .invalidCount(0)
                .build();

        when(partInboundAppService.processInbound(any(), eq(InboundSourceType.MES), any()))
                .thenReturn(expectedResult);

        // When
        ImportResult result = simParser.parse("BATCH001", dataJson);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        verify(partInboundAppService).processInbound(any(), eq(InboundSourceType.MES), any());
    }

    @Test
    @DisplayName("BTM解析器应处理空SN记录")
    void btmParser_shouldHandleBlankSn() {
        // Given
        BtmDataParserV1_0 btmParser = createBtmParser();

        JSONObject dataJson = buildBtmDataJson("SUP001", 
                new String[][]{
                    {"PN001", "", "HSM001", "MAC001"},  // 空SN
                    {"PN002", "SN002", "HSM002", "MAC002"}  // 有效SN
                }
        );

        PartInboundResult expectedResult = PartInboundResult.builder()
                .totalCount(1)
                .successCount(1)
                .failureCount(0)
                .invalidCount(0)
                .build();

        when(partInboundAppService.processInbound(any(), eq(InboundSourceType.MES), any()))
                .thenReturn(expectedResult);

        // When
        ImportResult result = btmParser.parse("BATCH001", dataJson);

        // Then
        assertNotNull(result);
        // 只有1条有效记录被传入内核
        verify(partInboundAppService).processInbound(argThat(records -> records.size() == 1), any(), any());
    }

    // ========== 辅助方法 ==========

    private BtmDataParserV1_0 createBtmParser() {
        BtmDataParserV1_0 parser = new BtmDataParserV1_0(idkBtmInfoService, parserRegistry);
        injectField(parser, "partInboundAppService", partInboundAppService);
        parser.init();
        return parser;
    }

    private TboxDataParserV1_0 createTboxParser() {
        TboxDataParserV1_0 parser = new TboxDataParserV1_0(tspTboxInfoService, parserRegistry);
        injectField(parser, "partInboundAppService", partInboundAppService);
        parser.init();
        return parser;
    }

    private CcpDataParserV1_0 createCcpParser() {
        CcpDataParserV1_0 parser = new CcpDataParserV1_0(tspCcpInfoService, parserRegistry);
        injectField(parser, "partInboundAppService", partInboundAppService);
        parser.init();
        return parser;
    }

    private IdcmDataParserV1_0 createIdcmParser() {
        IdcmDataParserV1_0 parser = new IdcmDataParserV1_0(tspIdcmInfoService, parserRegistry);
        injectField(parser, "partInboundAppService", partInboundAppService);
        parser.init();
        return parser;
    }

    private SimDataParserV1_0 createSimParser() {
        SimDataParserV1_0 parser = new SimDataParserV1_0(tspSimService, parserRegistry);
        injectField(parser, "partInboundAppService", partInboundAppService);
        parser.init();
        return parser;
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = BaseParser.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JSONObject buildBtmDataJson(String supplier, String[][] items) {
        JSONObject root = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject head = new JSONObject();
        head.set("ACCOUNT", supplier);
        JSONObject data = new JSONObject();
        cn.hutool.json.JSONArray itemsArray = new cn.hutool.json.JSONArray();
        for (String[] item : items) {
            JSONObject itemJson = new JSONObject();
            itemJson.set("NO", item[0]);
            itemJson.set("SN", item[1]);
            itemJson.set("HSM", item[2]);
            itemJson.set("MAC", item[3]);
            itemsArray.add(itemJson);
        }
        data.set("ITEMS", itemsArray);
        request.set("HEAD", head);
        request.set("DATA", data);
        root.set("REQUEST", request);
        return root;
    }

    private JSONObject buildTboxDataJson(String supplier, String[][] items) {
        JSONObject root = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject head = new JSONObject();
        head.set("ACCOUNT", supplier);
        JSONObject data = new JSONObject();
        cn.hutool.json.JSONArray itemsArray = new cn.hutool.json.JSONArray();
        for (String[] item : items) {
            JSONObject itemJson = new JSONObject();
            itemJson.set("NO", item[0]);
            itemJson.set("SN", item[1]);
            itemJson.set("ICCID1", item[2]);
            itemJson.set("ICCID2", item[3]);
            itemJson.set("IMEI", item[4]);
            itemJson.set("HSM", item[5]);
            itemsArray.add(itemJson);
        }
        data.set("ITEMS", itemsArray);
        request.set("HEAD", head);
        request.set("DATA", data);
        root.set("REQUEST", request);
        return root;
    }

    private JSONObject buildCcpDataJson(String supplier, String[][] items) {
        JSONObject root = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject head = new JSONObject();
        head.set("ACCOUNT", supplier);
        JSONObject data = new JSONObject();
        cn.hutool.json.JSONArray itemsArray = new cn.hutool.json.JSONArray();
        for (String[] item : items) {
            JSONObject itemJson = new JSONObject();
            itemJson.set("NO", item[0]);
            itemJson.set("SN", item[1]);
            itemJson.set("HSM", item[2]);
            itemsArray.add(itemJson);
        }
        data.set("ITEMS", itemsArray);
        request.set("HEAD", head);
        request.set("DATA", data);
        root.set("REQUEST", request);
        return root;
    }

    private JSONObject buildIdcmDataJson(String supplier, String[][] items) {
        JSONObject root = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject head = new JSONObject();
        head.set("ACCOUNT", supplier);
        JSONObject data = new JSONObject();
        cn.hutool.json.JSONArray itemsArray = new cn.hutool.json.JSONArray();
        for (String[] item : items) {
            JSONObject itemJson = new JSONObject();
            itemJson.set("NO", item[0]);
            itemJson.set("SN", item[1]);
            itemJson.set("HSM", item[2]);
            itemJson.set("MAC", item[3]);
            itemsArray.add(itemJson);
        }
        data.set("ITEMS", itemsArray);
        request.set("HEAD", head);
        request.set("DATA", data);
        root.set("REQUEST", request);
        return root;
    }

    private JSONObject buildSimDataJson(String mno, String[][] items) {
        JSONObject root = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject head = new JSONObject();
        JSONObject data = new JSONObject();
        data.set("MNO", mno);
        cn.hutool.json.JSONArray itemsArray = new cn.hutool.json.JSONArray();
        for (String[] item : items) {
            JSONObject itemJson = new JSONObject();
            itemJson.set("ICCID", item[0]);
            itemJson.set("IMSI", item[1]);
            itemJson.set("MSISDN", item[2]);
            itemsArray.add(itemJson);
        }
        data.set("ITEMS", itemsArray);
        request.set("HEAD", head);
        request.set("DATA", data);
        root.set("REQUEST", request);
        return root;
    }
}
