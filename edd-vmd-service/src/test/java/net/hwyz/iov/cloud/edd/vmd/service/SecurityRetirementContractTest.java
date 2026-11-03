package net.hwyz.iov.cloud.edd.vmd.service;

import net.hwyz.iov.cloud.edd.vmd.api.service.VmdVehiclePartBindingService;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehiclePartBindingChangedEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ImmoKeyDeliveryAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.OtaRootDeliveryAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartSecurityPresetAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.SecurityProvisionConfirmService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleSecurityPresetAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaLogicalTopic;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicDefinitionProvider;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicRoutes;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VMD-DSN-CR-074 退役契约测试
 * <p>
 * 证明：旧证书/Keyprov 入口不再注册、绑定契约保留、旧安全 Topic 退出新业务生产集、
 * 制造安全（根预置/灌注/KeyProvisioningTemplate 共享能力）未被误删。
 *
 * @author hwyz_leo
 */
@SpringBootTest
class SecurityRetirementContractTest {

    /**
     * CR-074 迁出前 VMD 安全入口映射前缀（MPT/Open/Service 三层）。
     */
    private static final Set<String> RETIRED_ENTRY_POINT_PREFIXES = Set.of(
            "/api/mpt/vehicleCertificate/v1",
            "/api/mpt/businessKey/v1",
            "/api/mpt/business-key/provisioning",
            "/api/open/vehicleCertificate/v1",
            "/api/open/businessKey/v1",
            "/api/service/keyProvisioning/v1",
            "/api/service/businessKeyDirectory/v1"
    );

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private VmdKafkaTopicProperties topicProperties;

    @Autowired
    private VmdKafkaTopicDefinitionProvider topicDefinitionProvider;

    @Test
    @DisplayName("旧证书/Keyprov 的 MPT、Open、Service 映射不再注册")
    void retiredSecurityEntryPoints_notRegistered() {
        Set<String> registered = requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .collect(Collectors.toSet());

        for (String prefix : RETIRED_ENTRY_POINT_PREFIXES) {
            assertFalse(registered.stream().anyMatch(p -> p.startsWith(prefix)),
                    "迁出入口不应再注册: " + prefix);
        }
    }

    @Test
    @DisplayName("绑定契约保留：VmdVehiclePartBindingService 契约与 /api/service/vehiclePartBinding/v1 映射仍在")
    void bindingContract_retained() {
        // VIAM 依赖的官方 Feign 契约仍存在（编译级保证；此处验证映射仍注册）
        Set<String> registered = requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .collect(Collectors.toSet());
        assertTrue(registered.stream().anyMatch(p -> p.startsWith("/api/service/vehiclePartBinding/v1")),
                "绑定查询入口必须保留（VIAM 只读消费）");
        // 绑定事实事件仍保留（SSOT 不迁）
        assertNotNull(VehiclePartBindingChangedEvent.class);
        assertNotNull(VmdVehiclePartBindingService.class);
    }

    @Test
    @DisplayName("旧安全 Topic 退出新业务生产集；保留生产 Topic 正常声明")
    void retiredTopics_notInProductionSet() {
        // 逻辑目录不含迁出 Topic
        for (VmdKafkaLogicalTopic logical : VmdKafkaLogicalTopic.values()) {
            assertFalse(logical.configKey().equals("business-key-changed")
                            || logical.configKey().equals("tbox-v2c-key-provisioned"),
                    "迁出 Topic 不得出现在逻辑目录: " + logical.configKey());
        }

        // 生产声明集 = 4 个保留 Topic，不含 vmd.business-key.changed / vmd.tbox-v2c-key-provisioned
        Set<String> produced = topicDefinitionProvider.topicDefinitions().stream()
                .map(KafkaTopicDefinition::name)
                .collect(Collectors.toSet());
        assertFalse(produced.contains("vmd.business-key.changed"), "VMD 不得再声明/生产业务密钥事件 Topic");
        assertFalse(produced.contains("vmd.tbox-v2c-key-provisioned"), "VMD 不得再声明/生产 V2C 开通事件 Topic");
        assertTrue(produced.contains("vmd.vehicle-part-binding.changed"), "绑定变更 Topic 必须保留");
        assertTrue(produced.contains("vmd.vehicle-produce"), "车辆生产 Topic 必须保留");
        assertTrue(produced.contains("vmd.vehicle-software-inventory.changed"), "软件清单 Topic 必须保留");
        assertTrue(produced.contains("vmd.part-inbound.changed"), "零件入站 Topic 必须保留");
        assertEquals(4, produced.size(), "VMD 生产 Topic 应恰为 4 个（枚举 PRODUCER_OWNED）");

        // 路由注册表不再解析迁出事件类型（fail-fast 由 VmdKafkaTopicRoutes 保证）
        VmdKafkaTopicRoutes routes = new VmdKafkaTopicRoutes(topicProperties);
        assertFalse(routes.isRegistered("BusinessKeyChangedEvent"));
        assertFalse(routes.isRegistered("TboxV2cKeyProvisionedEvent"));
    }

    @Test
    @DisplayName("制造安全回归：根预置/灌注/共享 KeyProvisioningTemplate 能力未误删")
    void manufacturingSecurity_retained() {
        assertNotNull(partSecurityPresetAppService, "零件安全常量预置必须保留（产线根预置）");
        assertNotNull(vehicleSecurityPresetAppService, "车辆安全常量预置必须保留（产线根预置）");
        assertNotNull(securityProvisionConfirmService, "EOL 灌注确认必须保留");
        assertNotNull(otaRootDeliveryAppService, "OTA 根灌注必须保留");
        assertNotNull(immoKeyDeliveryAppService, "IMMO 根灌注必须保留");
        assertNotNull(keyProvisioningTemplate, "KeyProvisioningTemplate（crypto.provisioning.enabled）必须保留");
    }

    @Autowired
    private PartSecurityPresetAppService partSecurityPresetAppService;

    @Autowired
    private VehicleSecurityPresetAppService vehicleSecurityPresetAppService;

    @Autowired
    private SecurityProvisionConfirmService securityProvisionConfirmService;

    @Autowired
    private OtaRootDeliveryAppService otaRootDeliveryAppService;

    @Autowired
    private ImmoKeyDeliveryAppService immoKeyDeliveryAppService;

    @Autowired
    private KeyProvisioningTemplate keyProvisioningTemplate;
}
