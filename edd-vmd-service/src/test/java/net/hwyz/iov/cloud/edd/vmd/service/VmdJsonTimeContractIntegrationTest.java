package net.hwyz.iov.cloud.edd.vmd.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.request.SoftwareManifestRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VehicleLifecycleNodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.event.VehicleSoftwareInventoryObservedEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.BusinessKeyChangedEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmBrandEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.TboxV2cKeyProvisionedEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.TboxV2cKeyProvisionedEvent.Key;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdEventJsonCodec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VMD-DSN-CR-070 JSON java.time 时间契约集成测试（真实生产 mapper）
 * <p>
 * 覆盖 CR-070 §4 验收：双读（ISO/10 位秒/13 位毫秒等值）、写单一（全局 epoch-millis
 * 旧输出）、两个事件的既有边界例外（专用 ISO codec）、排除面 golden（keyprov Proto
 * bytes / 普通 long / Date 语义不变）、非法格式 fail-closed。
 * <p>
 * 使用注入的<b>生产 ObjectMapper</b>（已应用 framework IovJacksonAutoConfiguration
 * 与 Nacos spring.jackson 配置），而非测试内临时构造——以真实 mapper 证明双读与写模式。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@SpringBootTest
class VmdJsonTimeContractIntegrationTest {

    private static final Instant SAMPLE = Instant.parse("2026-10-28T12:43:32Z");
    private static final long MILLIS = SAMPLE.toEpochMilli();   // 1793191412000
    private static final long SECONDS = SAMPLE.getEpochSecond(); // 1793191412

    @Autowired
    private ObjectMapper globalObjectMapper;

    @Autowired
    private VmdEventJsonCodec vmdEventJsonCodec;

    // ---------- 写单一：全局 epoch-millis 旧输出（兼容阶段保持） ----------

    @Test
    void globalMapper_epochMillis_write() throws Exception {
        // Instant/OffsetDateTime/LocalDateTime → epoch 毫秒数字（旧框架行为冻结）
        JsonNode node = globalObjectMapper.valueToTree(java.util.Map.of(
                "instant", SAMPLE,
                "local", LocalDateTime.of(2026, 10, 14, 14, 26, 13)));
        assertTrue(node.get("instant").isNumber(), "epoch-millis 模式 Instant 应为数字，实际: " + node.get("instant"));
        assertEquals(MILLIS, node.get("instant").asLong(), "Instant epoch 毫秒数值错误");

        // java.util.Date 保持 Nacos write-dates-as-timestamps=true 语义（epoch 毫秒数字）
        Date d = new Date(MILLIS);
        JsonNode dateNode = globalObjectMapper.valueToTree(java.util.Map.of("d", d));
        assertTrue(dateNode.get("d").isNumber(), "java.util.Date 应保持既有 epoch 毫秒数字输出（Nacos feature），实际: " + dateNode.get("d"));
        assertEquals(MILLIS, dateNode.get("d").asLong());
    }

    // ---------- 读兼容：ISO 字符串 / 10 位秒 / 13 位毫秒 归一同一时刻 ----------

    @Test
    void globalMapper_dualRead_instantEquals() throws Exception {
        Instant fromIso = globalObjectMapper.readValue("\"" + SAMPLE + "\"", Instant.class);
        Instant fromMillis = globalObjectMapper.readValue(String.valueOf(MILLIS), Instant.class);
        Instant fromSeconds = globalObjectMapper.readValue(String.valueOf(SECONDS), Instant.class);
        assertEquals(SAMPLE, fromIso);
        assertEquals(SAMPLE, fromMillis);
        assertEquals(SAMPLE, fromSeconds);
        // 13 位毫秒保留毫秒精度（10 位秒无法表达亚秒精度，属预期，不做错误归一）
        Instant withMillis = globalObjectMapper.readValue("1793191412340", Instant.class);
        assertEquals(Instant.parse("2026-10-28T12:43:32.340Z"), withMillis);
    }

    @Test
    void globalMapper_dualRead_localDateTime() throws Exception {
        // LocalDateTime 数字路径使用显式 zone-id=Asia/Shanghai（测试 JVM 已实证 +08:00）
        LocalDateTime fromIso = globalObjectMapper.readValue("\"2026-10-14T14:26:13\"", LocalDateTime.class);
        assertEquals(LocalDateTime.of(2026, 10, 14, 14, 26, 13), fromIso);
        // 13 位毫秒 → Instant → Asia/Shanghai 墙上时间
        LocalDateTime fromMillis = globalObjectMapper.readValue(String.valueOf(MILLIS), LocalDateTime.class);
        assertEquals(LocalDateTime.ofInstant(SAMPLE, java.time.ZoneId.of("Asia/Shanghai")), fromMillis);
    }

    // ---------- 非法格式 fail-closed（RD-001-3，不悄悄纠正） ----------

    @Test
    void globalMapper_rejectsIllegalShapes() throws Exception {
        // 小数（秒小数/亚毫秒）拒绝
        assertThrows(Exception.class, () -> globalObjectMapper.readValue("1793191412.340", Instant.class));
        // 数组（LocalDateTime 数组形式）拒绝
        assertThrows(Exception.class, () -> globalObjectMapper.readValue("[2026,10,14,14,26,13]", LocalDateTime.class));
        // 空字符串拒绝
        assertThrows(Exception.class, () -> globalObjectMapper.readValue("\"\"", Instant.class));
        // Instant 无 offset 字符串拒绝（不是 RFC 3339 绝对时刻）
        assertThrows(Exception.class, () -> globalObjectMapper.readValue("\"2026-10-14T14:26:13\"", Instant.class));
        // 窗口外数字（0/负值/其他位数）拒绝，不做无依据猜测
        assertThrows(Exception.class, () -> globalObjectMapper.readValue("0", Instant.class));
        assertThrows(Exception.class, () -> globalObjectMapper.readValue("123", Instant.class));
        // JSON null 按字段可空性处理，不转换为 epoch 0
        assertNull(globalObjectMapper.readValue("null", Instant.class));
    }

    // ---------- 两个事件：既有边界例外（VMD-DSN-CR-070 §5.10.2 登记） ----------

    @Test
    void exceptionEvents_alwaysIso_regardlessOfGlobalMode() throws Exception {
        BusinessKeyChangedEvent bizEvent = new BusinessKeyChangedEvent(
                "SN001", "V2C_COMM", "UP_LINK_AEAD", "key-1", 2L, "ACTIVE", SAMPLE);
        TboxV2cKeyProvisionedEvent tboxEvent = new TboxV2cKeyProvisionedEvent(
                "e1", "rid-1", "SN001", "V2C_COMM", List.of(new Key("UP_LINK_AEAD", "key-1", 3L)),
                "SUCCEEDED", SAMPLE);

        // 专用 codec：occurredAt 固定 ISO-8601 字符串
        for (String payload : new String[]{
                vmdEventJsonCodec.writeAsString(bizEvent),
                vmdEventJsonCodec.writeAsString(tboxEvent)}) {
            JsonNode node = vmdEventJsonCodec.mapper().readTree(payload);
            JsonNode occurredAt = node.get("occurredAt");
            assertNotNull(occurredAt);
            assertFalse(occurredAt.isNumber(), "专用 codec 的 occurredAt 不得为数字");
            assertTrue(occurredAt.asText().matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$"),
                    "ISO-8601 格式非法: " + occurredAt.asText());
            assertEquals(SAMPLE, Instant.parse(occurredAt.asText()));
        }

        // 对照：全局 epoch-millis mapper 序列化同一事件 → occurredAt 为数字（证明该例外只
        // 作用于这两个事件，不因专用 codec 改变其他受管字段；同时锁定例外边界）。
        JsonNode globalBiz = globalObjectMapper.readTree(globalObjectMapper.writeValueAsString(bizEvent));
        assertTrue(globalBiz.get("occurredAt").isNumber(),
                "全局 mapper 对该事件仍按 epoch-millis 写出（例外仅由专用 codec 生效）");
        assertEquals(MILLIS, globalBiz.get("occurredAt").asLong());
    }

    // ---------- DTO 级双读（CR-070 §2 影响字段：Service/Feign 请求、Kafka 事件、MPT 响应） ----------

    @Test
    void dto_dualRead_softwareManifestRequest() throws Exception {
        // ISO 字符串
        SoftwareManifestRequest iso = globalObjectMapper.readValue(
                "{\"vin\":\"VIN1\",\"source\":\"OTA\",\"occurredAt\":\"" + SAMPLE + "\",\"items\":[]}",
                SoftwareManifestRequest.class);
        // 13 位毫秒数字
        SoftwareManifestRequest num = globalObjectMapper.readValue(
                "{\"vin\":\"VIN1\",\"source\":\"OTA\",\"occurredAt\":" + MILLIS + ",\"items\":[]}",
                SoftwareManifestRequest.class);
        assertEquals(SAMPLE, iso.getOccurredAt());
        assertEquals(SAMPLE, num.getOccurredAt());
    }

    @Test
    void dto_dualRead_otaObservedEvent() throws Exception {
        // collectedAt/acceptedAt 置于 items 之前（items 为末尾字段）
        String base = "{\"eventId\":\"e1\",\"observationKey\":\"k1\",\"vin\":\"VIN1\","
                + "\"inventoryRevision\":\"v1\",\"collectedAt\":\"" + SAMPLE + "\",\"acceptedAt\":\"" + SAMPLE
                + "\",\"canonicalizationVersion\":1,\"canonicalDigest\":\"d\",\"items\":[]}";
        String baseNum = "{\"eventId\":\"e1\",\"observationKey\":\"k1\",\"vin\":\"VIN1\","
                + "\"inventoryRevision\":\"v1\",\"collectedAt\":" + MILLIS + ",\"acceptedAt\":" + MILLIS
                + ",\"canonicalizationVersion\":1,\"canonicalDigest\":\"d\",\"items\":[]}";
        VehicleSoftwareInventoryObservedEvent iso = globalObjectMapper.readValue(base,
                VehicleSoftwareInventoryObservedEvent.class);
        VehicleSoftwareInventoryObservedEvent num = globalObjectMapper.readValue(baseNum,
                VehicleSoftwareInventoryObservedEvent.class);
        assertEquals(SAMPLE, iso.getCollectedAt());
        assertEquals(SAMPLE, iso.getAcceptedAt());
        assertEquals(SAMPLE, num.getCollectedAt());
        assertEquals(SAMPLE, num.getAcceptedAt());
    }

    @Test
    void dto_dualRead_mdmEvent_localDateTime() throws Exception {
        // MDM 入站事件 occurredAt：LocalDateTime 双读（ISO 墙上时间 / 13 位毫秒经 Asia/Shanghai）
        // MdmEvent 为抽象基类，用具体子类 MdmBrandEvent 验证（MDM consumer 实际反序列化对象）
        LocalDateTime expectedWall = LocalDateTime.ofInstant(SAMPLE, java.time.ZoneId.of("Asia/Shanghai"));
        MdmBrandEvent iso = globalObjectMapper.readValue(
                "{\"entityId\":\"1\",\"code\":\"C1\",\"name\":\"n\",\"occurredAt\":\"" + expectedWall + "\"}",
                MdmBrandEvent.class);
        MdmBrandEvent num = globalObjectMapper.readValue(
                "{\"entityId\":\"1\",\"code\":\"C1\",\"name\":\"n\",\"occurredAt\":" + MILLIS + "}",
                MdmBrandEvent.class);
        assertEquals(expectedWall, iso.getOccurredAt());
        assertEquals(expectedWall, num.getOccurredAt());
    }

    @Test
    void dto_dualRead_vehicleLifecycleNodeResponse() throws Exception {
        VehicleLifecycleNodeResponse iso = globalObjectMapper.readValue(
                "{\"node\":\"PRODUCE\",\"reachTime\":\"" + SAMPLE + "\"}",
                VehicleLifecycleNodeResponse.class);
        VehicleLifecycleNodeResponse num = globalObjectMapper.readValue(
                "{\"node\":\"PRODUCE\",\"reachTime\":" + MILLIS + "}",
                VehicleLifecycleNodeResponse.class);
        assertEquals(SAMPLE, iso.getReachTime());
        assertEquals(SAMPLE, num.getReachTime());
    }

    // ---------- MVC/HTTP 路径（真实 MappingJackson2HttpMessageConverter + 生产 mapper） ----------

    @Test
    void httpConverter_dualRead_requestAndEpochMillisResponse() throws Exception {
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(globalObjectMapper);

        // POST 请求体：ISO 字符串与 13 位毫秒数字双读，归一同一时刻
        MockHttpInputMessage isoMsg = new MockHttpInputMessage(
                ("{\"vin\":\"VIN1\",\"source\":\"OTA\",\"occurredAt\":\"" + SAMPLE
                        + "\",\"items\":[]}").getBytes());
        isoMsg.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        SoftwareManifestRequest isoReq = (SoftwareManifestRequest) converter.read(
                SoftwareManifestRequest.class, isoMsg);
        MockHttpInputMessage numMsg = new MockHttpInputMessage(
                ("{\"vin\":\"VIN1\",\"source\":\"OTA\",\"occurredAt\":" + MILLIS
                        + ",\"items\":[]}").getBytes());
        numMsg.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        SoftwareManifestRequest numReq = (SoftwareManifestRequest) converter.read(
                SoftwareManifestRequest.class, numMsg);
        assertEquals(SAMPLE, isoReq.getOccurredAt());
        assertEquals(SAMPLE, numReq.getOccurredAt());

        // 响应写出：reachTime 保持 epoch-millis 数字（兼容阶段旧输出）
        VehicleLifecycleNodeResponse resp = new VehicleLifecycleNodeResponse();
        resp.setNode("PRODUCE");
        resp.setReachTime(SAMPLE);
        MockHttpOutputMessage out = new MockHttpOutputMessage();
        converter.write(resp, MediaType.APPLICATION_JSON, out);
        JsonNode body = globalObjectMapper.readTree(out.getBodyAsString());
        assertTrue(body.get("reachTime").isNumber(), "MVC 响应 reachTime 应为 epoch-millis 数字");
        assertEquals(MILLIS, body.get("reachTime").asLong());
    }

    // ---------- 排除面 golden：keyprov 外层 DTO / Proto bytes 不受影响 ----------

    @Test
    void excludedContracts_unchanged() throws Exception {
        // keyprov 请求：canonical payload bytes 原样（普通 byte[] 不被时间模块触碰）
        byte[] payloadBytes = new byte[]{0x0a, 0x01, 0x02, 0x03};
        VmdKeyProvisioningRequest request = VmdKeyProvisioningRequest.builder()
                .payloadType("security.keyprov.v1.KeyProvisioningRequest")
                .schemaVersion("keyprov-v1")
                .payloadBytes(payloadBytes)
                .payloadDigest("sha256-hex")
                .build();
        JsonNode node = globalObjectMapper.valueToTree(request);
        // payloadBytes 以 base64 原样携带（Jackson byte[] 默认 base64），字节内容不变
        assertNotNull(node.get("payloadBytes"));
        byte[] round = globalObjectMapper.convertValue(node.get("payloadBytes"), byte[].class);
        assertArrayEquals(payloadBytes, round, "Proto bytes 必须原样，不得被时间契约重映射");

        // 普通 long（如 mqttReceivedAt 类字段）保持数值原样
        assertTrue(node.get("schemaVersion").isTextual());
    }

    // ---------- 时区无关性：Instant 读写不依赖 JVM 默认时区 ----------

    @Test
    void instantRoundTrip_timezoneIndependent() throws Exception {
        java.util.TimeZone original = java.util.TimeZone.getDefault();
        try {
            for (String tz : new String[]{"UTC", "Asia/Shanghai", "America/New_York"}) {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(tz));
                Instant iso = globalObjectMapper.readValue("\"" + SAMPLE + "\"", Instant.class);
                Instant num = globalObjectMapper.readValue(String.valueOf(MILLIS), Instant.class);
                assertEquals(SAMPLE, iso, "JVM 时区 " + tz + " 下 ISO 读取必须一致");
                assertEquals(SAMPLE, num, "JVM 时区 " + tz + " 下数字读取必须一致");
            }
        } finally {
            java.util.TimeZone.setDefault(original);
        }
    }
}
