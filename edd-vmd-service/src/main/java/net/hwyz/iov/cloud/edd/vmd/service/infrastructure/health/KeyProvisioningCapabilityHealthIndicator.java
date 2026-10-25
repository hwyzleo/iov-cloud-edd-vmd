package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.health;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.adapter.KeyProvisioningServiceAdapter;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyPolicyRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.DerivedKeyProviderReadinessGate;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyprovConstants;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyProvisioningResponseStore;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ParProtoVectorFixture;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ParProtoWireMapperV1;
import net.hwyz.iov.cloud.framework.security.crypto.DerivedEnvelopeCapability;
import net.hwyz.iov.cloud.framework.security.crypto.ImportProofCapability;
import net.hwyz.iov.cloud.framework.security.crypto.ImportProofVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.codec.DerivedEnvelopeDigestVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateReferenceDirectory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Keyprov capability 独立健康检查（VMD-DSN-CR-059 §7）。
 * <p>
 * 与 VMD 其他能力解耦：本指示器不进入主/全局 readiness 聚合（由
 * {@link KeyProvisioningHealthGroupsCustomizer} 从 primary group 排除），仅通过独立
 * health group {@code keyprov}（/actuator/health/keyprov）对外呈现，供 VAGW 与发布门禁判断
 * 是否启用 Keyprov 流量。
 * <ul>
 *   <li>enabled=false：状态 DISABLED，视为 UP（能力未装配，非故障）</li>
 *   <li>enabled=true：Controller/Adapter/Facade、V2C 业务策略、framework-security 能力或
 *       必要表结构缺失时 DOWN，禁止返回模拟成功</li>
 * </ul>
 * CR-061：V2C 材料能力门禁由随机 {@code BusinessKeyMaterialTemplate} 改为根派生
 * {@link DerivedKeyProviderReadinessGate}（Transit/HSM {@code v2c-comm-root}）。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
@Slf4j
@Component
@ConditionalOnClass(HealthIndicator.class)
public class KeyProvisioningCapabilityHealthIndicator implements HealthIndicator {

    private final ObjectProvider<KeyProvisioningServiceAdapter> adapterProvider;
    private final BusinessKeyPolicyRegistry policyRegistry;
    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;
    private final ObjectProvider<CertificateReferenceDirectory> certificateDirectoryProvider;
    private final DerivedKeyProviderReadinessGate derivedKeyProviderReadinessGate;
    private final ObjectProvider<DerivedEnvelopeDigestVerifier> envelopeDigestVerifierProvider;
    private final ObjectProvider<ImportProofVerifier> importProofVerifierProvider;
    // CR-068：外部 AAD profile / Wire Mapper / Response 重放存储
    private final ObjectProvider<net.hwyz.iov.cloud.framework.security.crypto.provider.EnvelopeAadEncoderRegistry> aadEncoderRegistryProvider;
    private final ObjectProvider<net.hwyz.iov.cloud.framework.security.crypto.provider.OperationResultStore> operationResultStoreProvider;
    private final ObjectProvider<ParProtoWireMapperV1> wireMapperProvider;
    private final ObjectProvider<KeyProvisioningResponseStore> responseStoreProvider;

    @Value("${vmd.business-key.provisioning.keyprov.enabled:false}")
    private boolean keyprovEnabled;

    @Value("${vmd.business-key.provisioning.keyprov.protocol-release:}")
    private String protocolRelease;

    @Value("${vmd.business-key.provisioning.keyprov.descriptor-set-hash:}")
    private String descriptorSetHash;

    /**
     * KL1 capability 开关（kill-switch；开启时校验 KL1 必要表结构，VMD-DSN-CR-069 §6）
     */
    @Value("${vmd.business-key.provisioning.kl1.enabled:false}")
    private boolean kl1Enabled;

    /**
     * AAD profile id（必须等于 PAR_PROTO_KEYPROV_AAD_V1；缺失/不一致时外部 AAD 能力 NOT_READY）
     */
    @Value("${crypto.provisioning.derived-key.aad-profile-id:}")
    private String aadProfileId;

    public KeyProvisioningCapabilityHealthIndicator(ObjectProvider<KeyProvisioningServiceAdapter> adapterProvider,
                                                    BusinessKeyPolicyRegistry policyRegistry,
                                                    ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
                                                    ObjectProvider<CertificateReferenceDirectory> certificateDirectoryProvider,
                                                    DerivedKeyProviderReadinessGate derivedKeyProviderReadinessGate,
                                                    ObjectProvider<DerivedEnvelopeDigestVerifier> envelopeDigestVerifierProvider,
                                                    ObjectProvider<ImportProofVerifier> importProofVerifierProvider,
                                                    ObjectProvider<net.hwyz.iov.cloud.framework.security.crypto.provider.EnvelopeAadEncoderRegistry> aadEncoderRegistryProvider,
                                                    ObjectProvider<net.hwyz.iov.cloud.framework.security.crypto.provider.OperationResultStore> operationResultStoreProvider,
                                                    ObjectProvider<ParProtoWireMapperV1> wireMapperProvider,
                                                    ObjectProvider<KeyProvisioningResponseStore> responseStoreProvider) {
        this.adapterProvider = adapterProvider;
        this.policyRegistry = policyRegistry;
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.certificateDirectoryProvider = certificateDirectoryProvider;
        this.derivedKeyProviderReadinessGate = derivedKeyProviderReadinessGate;
        this.envelopeDigestVerifierProvider = envelopeDigestVerifierProvider;
        this.importProofVerifierProvider = importProofVerifierProvider;
        this.aadEncoderRegistryProvider = aadEncoderRegistryProvider;
        this.operationResultStoreProvider = operationResultStoreProvider;
        this.wireMapperProvider = wireMapperProvider;
        this.responseStoreProvider = responseStoreProvider;
    }

    @Override
    public Health health() {
        if (!keyprovEnabled) {
            return Health.up()
                    .withDetail("state", "DISABLED")
                    .withDetail("note", "vmd.business-key.provisioning.keyprov.enabled=false，Keyprov capability 未装配")
                    .build();
        }
        Health.Builder builder = Health.up().withDetail("state", "READY");
        boolean down = false;

        if (adapterProvider.getIfAvailable() == null) {
            down = down(builder, "adapter", "KeyProvisioningServiceAdapter 缺失");
        }
        if (certificateDirectoryProvider.getIfAvailable() == null) {
            down = down(builder, "certificateDirectory", "证书目录 Adapter 未装配（CertificateReferenceDirectory 缺失）");
        }
        if (!derivedKeyProviderReadinessGate.isReady()) {
            down = down(builder, "derivedKeyProvider",
                    "根派生/Transit root capability 未就绪: " + derivedKeyProviderReadinessGate.reason());
        }
        // CR-062 §9：envelope 公开 canonical verifier 与 ImportProofVerifier capability 均需 READY
        if (envelopeDigestVerifierProvider.getIfAvailable() == null
                || !DerivedEnvelopeCapability.coreDigestVerifyReady()) {
            down = down(builder, "envelopeDigestVerifier",
                    "结构化信封 canonical verifier 未就绪（STRUCTURED_ENVELOPE_VIEW_V1/CORE_ENVELOPE_DIGEST_VERIFY_V1）");
        }
        if (importProofVerifierProvider.getIfAvailable() == null || !ImportProofCapability.selfCheckReady()) {
            down = down(builder, "importProofVerifier",
                    "ImportProofVerifier 未装配或 IMPORT_PROOF_VERIFY_ECDSA_P256_V1 自检未通过");
        }
        if (protocolRelease == null || protocolRelease.isBlank()
                || descriptorSetHash == null || descriptorSetHash.isBlank()) {
            down = down(builder, "releaseSet", "keyprov release/DescriptorSet hash 未固定，envelope v1 能力未就绪");
        }
        // CR-068 §6：外部 AAD profile（FW-SEC CR-020）——profile 已配置、registry 已装配并含该 profile、
        // probe 自检通过、Operation Result Store 可用
        net.hwyz.iov.cloud.framework.security.crypto.provider.EnvelopeAadEncoderRegistry aadRegistry =
                aadEncoderRegistryProvider.getIfAvailable();
        net.hwyz.iov.cloud.framework.security.crypto.provider.OperationResultStore resultStore =
                operationResultStoreProvider.getIfAvailable();
        String aadReason = net.hwyz.iov.cloud.framework.security.crypto.DerivedEnvelopeCapability
                .externalAadProfileReadyReason(aadRegistry, aadProfileId, resultStore, true);
        if (aadReason != null) {
            down = down(builder, "externalAadProfileReady", "FW_EXTERNAL_AAD_PROFILE_READY: " + aadReason);
        }
        // CR-068 §6：VMD PAR-PROTO Wire Mapper V1——装配 + 确定性自检（framework 外部 AAD profile 能力由
        // externalAadProfileReady 表达，此处不再重复声明 supports(PAR_PROTO_WIRE_MAPPER_V1)）
        ParProtoWireMapperV1 wireMapper = wireMapperProvider.getIfAvailable();
        if (wireMapper == null || !ParProtoWireMapperV1.deterministicSelfCheck()) {
            down = down(builder, "wireMapperV1", "VMD_PAR_PROTO_WIRE_MAPPER_V1_READY: Wire Mapper 未装配/确定性自检失败");
        }
        // CR-068 §6：PAR-PROTO CR-009 权威向量——本仓仅允许 vendored 官方向量作为 READY 证据；
        // 未 vendored（或 provisional fixture 不一致）时保持 DOWN，直至 PAR-PROTO 仓向量对齐
        if (!ParProtoVectorFixture.vectorMatched()) {
            down = down(builder, "cr009Vector", "PAR_PROTO_CR009_VECTOR_MATCHED: 官方权威向量未 vendored 或比对不一致"
                    + "（provisional fixture 不构成放行证据）");
        }
        // CR-068 §6：Response 字节级重放存储就绪（密钥已注入 + 仓库可用）
        KeyProvisioningResponseStore responseStore = responseStoreProvider.getIfAvailable();
        if (responseStore == null || !responseStore.productionReady()) {
            down = down(builder, "responseReplayStore", "KEYPROV_RESPONSE_REPLAY_STORE_READY: "
                    + "Response 重放存储未装配或加密密钥未注入");
        }
        // CR-069 §6：KL1 生命周期能力（开启时校验 V65 必要表结构/列；kill-switch 关闭时不阻塞）
        if (kl1Enabled) {
            JdbcTemplate kl1Jdbc = jdbcTemplateProvider.getIfAvailable();
            if (kl1Jdbc == null) {
                down = down(builder, "kl1", "KL1 就绪检查无法执行（JdbcTemplate 缺失）");
            } else {
                try {
                    kl1Jdbc.query("SELECT 1 FROM tb_keyprov_delivery_lease LIMIT 1", rs -> null);
                    kl1Jdbc.query("SELECT lifecycle_profile, first_issued_at, ack_deadline_at, "
                            + "immutable_content_digest, lease_seq FROM tb_business_key_provisioning LIMIT 1", rs -> null);
                    kl1Jdbc.query("SELECT item_wire_offset, item_wire_length "
                            + "FROM tb_business_key_provisioning_item LIMIT 1", rs -> null);
                } catch (RuntimeException e) {
                    down = down(builder, "kl1", "KL1 必要表结构缺失（V65 未迁移）: tb_keyprov_delivery_lease / KL1 列");
                }
            }
        }
        Set<String> purposes = policyRegistry.resolvePurposes(KeyprovConstants.BUSINESS_DOMAIN_V2C).orElse(Set.of());
        if (!KeyprovConstants.isCompleteV2cPurposeSet(purposes)) {
            down = down(builder, "policy", "V2C_COMM 业务策略缺失或用途不完整");
        }
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            down = down(builder, "schema", "JdbcTemplate 缺失，无法校验开通批次表结构");
        } else {
            try {
                jdbcTemplate.query("SELECT 1 FROM tb_business_key_provisioning LIMIT 1", rs -> null);
                jdbcTemplate.query("SELECT response_payload, response_digest, response_codec_version, response_persisted_at "
                        + "FROM tb_business_key_provisioning LIMIT 1", rs -> null);
                jdbcTemplate.query("SELECT core_envelope_digest, wire_envelope_digest, aad_profile_id, "
                        + "parameters_digest, wrapped_key_digest FROM tb_business_key_provisioning_item LIMIT 1", rs -> null);
            } catch (RuntimeException e) {
                down = down(builder, "schema", "必要表结构缺失: tb_business_key_provisioning/business_key_provisioning_item");
            }
        }
        return down ? builder.down().withDetail("state", "DOWN").build() : builder.build();
    }

    private boolean down(Health.Builder builder, String key, String reason) {
        builder.withDetail(key, "DOWN").withDetail(key + ".reason", reason);
        return true;
    }
}
