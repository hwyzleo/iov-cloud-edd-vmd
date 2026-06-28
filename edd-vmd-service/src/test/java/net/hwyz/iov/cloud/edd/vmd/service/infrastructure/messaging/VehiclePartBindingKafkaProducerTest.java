package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * 测试发送 VMD 车辆绑定变更 Kafka 消息
 * <p>
 * 模拟 TOL 导入绑定 TBOX 后发送的消息，验证 TSP 端消费处理
 *
 * @author hwyz_leo
 */
@Slf4j
public class VehiclePartBindingKafkaProducerTest extends BaseTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC = "vmd-vehicle-binding-changed";

    @Test
    @DisplayName("模拟TOL导入绑定TBOX后发送Kafka消息")
    void testSendTboxBindingChangedEvent() throws Exception {
        String testVin = "LFV2TEST000000001";

        // 构造 VehiclePartBindingChangedEvent JSON
        JSONObject event = new JSONObject();
        event.set("bindingId", 90001L);
        event.set("partCode", "TBOX_5G_001");
        event.set("sn", "SN_TBOX_TEST_001");
        event.set("deviceCategory", "TBOX");
        event.set("vehicleNodeCode", "TBOX_5G");
        event.set("changeType", "BIND");
        event.set("replaceOfBindingId", null);
        event.set("occurredAt", Instant.now().toString());
        event.set("seq", 900010001L);

        String eventJson = JSONUtil.toJsonStr(event);
        log.info("发送TBOX绑定变更消息到Kafka: topic={}, key={}, value={}", TOPIC, testVin, eventJson);

        kafkaTemplate.send(TOPIC, testVin, eventJson).get(10, TimeUnit.SECONDS);

        log.info("消息发送成功，请检查TSP服务日志");
        // 等待一下让TSP消费
        Thread.sleep(5000);
    }

    @Test
    @DisplayName("模拟TOL导入绑定CCP后发送Kafka消息")
    void testSendCcpBindingChangedEvent() throws Exception {
        String testVin = "LFV2TEST000000002";

        JSONObject event = new JSONObject();
        event.set("bindingId", 90002L);
        event.set("partCode", "CCP_001");
        event.set("sn", "SN_CCP_TEST_001");
        event.set("deviceCategory", "CCP");
        event.set("vehicleNodeCode", "CCP");
        event.set("changeType", "BIND");
        event.set("replaceOfBindingId", null);
        event.set("occurredAt", Instant.now().toString());
        event.set("seq", 900020001L);

        String eventJson = JSONUtil.toJsonStr(event);
        log.info("发送CCP绑定变更消息到Kafka: topic={}, key={}, value={}", TOPIC, testVin, eventJson);

        kafkaTemplate.send(TOPIC, testVin, eventJson).get(10, TimeUnit.SECONDS);

        log.info("消息发送成功，请检查TSP服务日志");
        Thread.sleep(5000);
    }

    @Test
    @DisplayName("模拟发送非TBOX/CCP设备类别消息（应被TSP忽略）")
    void testSendOtherDeviceCategoryEvent() throws Exception {
        String testVin = "LFV2TEST000000003";

        JSONObject event = new JSONObject();
        event.set("bindingId", 90003L);
        event.set("partCode", "CDC_001");
        event.set("sn", "SN_CDC_TEST_001");
        event.set("deviceCategory", "CDC");
        event.set("vehicleNodeCode", "CDC");
        event.set("changeType", "BIND");
        event.set("replaceOfBindingId", null);
        event.set("occurredAt", Instant.now().toString());
        event.set("seq", 900030001L);

        String eventJson = JSONUtil.toJsonStr(event);
        log.info("发送CDC绑定变更消息到Kafka: topic={}, key={}, value={}", TOPIC, testVin, eventJson);

        kafkaTemplate.send(TOPIC, testVin, eventJson).get(10, TimeUnit.SECONDS);

        log.info("消息发送成功，TSP应忽略此消息（非TBOX/CCP设备类别）");
        Thread.sleep(5000);
    }

}
