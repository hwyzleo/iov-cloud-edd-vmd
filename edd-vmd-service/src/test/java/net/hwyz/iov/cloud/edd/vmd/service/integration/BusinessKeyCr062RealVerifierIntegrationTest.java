package net.hwyz.iov.cloud.edd.vmd.service.integration;

import com.google.protobuf.ByteString;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningOutcome;
import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.KeyProvisioningOperationResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyProvisioningApplicationFacade;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyprovAuthenticatedContext;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyprovConstants;
import net.hwyz.iov.cloud.edd.vmd.service.support.KeyprovEnvelopeTestFixtures;
import net.hwyz.iov.cloud.edd.vmd.service.support.TestDeviceCertificate;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.codec.EcdsaP1363;
import net.hwyz.iov.cloud.framework.security.crypto.codec.ImportProofCanonical;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertResolver;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateEvidenceValidator;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateEvidenceValidator;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.DeviceCertificateRef;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.EvidenceSource;
import net.hwyz.iov.security.keyprov.v1.KeyEnvelopeFormat;
import net.hwyz.iov.security.keyprov.v1.KeyImportItemResult;
import net.hwyz.iov.security.keyprov.v1.KeyImportResult;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAck;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningRequest;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResponse;
import net.hwyz.iov.security.keyprov.v1.KeyPurpose;
import net.hwyz.iov.security.keyprov.v1.WrappingSuite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;

import java.security.MessageDigest;
import java.security.Signature;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * VMD-DSN-CR-062 真实 ImportProofVerifier 端到端集成测试（真实 dev MySQL + 真实 FW-SEC DefaultImportProofVerifier）。
 * <p>
 * 以 P-256 自签名设备证书生成 {@code proofInputV1} 的 low-S P1363 签名，贯通
 * VMD ACK 映射 → FW-SEC canonical 构建 → 证书证据/摘要绑定 → 真实 ECDSA-SHA256 验签 → 两用途原子激活。
 * 仅 {@link CertResolver} 以测试证书材料替身（隔离外部 PKI），验签器为真实生产实现。
 *
 * @author hwyz_leo
 * @since 2026-10-19
 */
@Rollback
@TestPropertySource(properties = {
        "crypto.import-proof.enabled=true",
        "crypto.import-proof.certificate.require-trusted-chain=false",
        "crypto.import-proof.certificate.require-good-status=false"
})
class BusinessKeyCr062RealVerifierIntegrationTest extends BaseTest {

    private static final String DOMAIN = "V2C_COMM";
    private static final String CALLER = "VAGW";
    private static final String HSM_UID = "CR062R-UID-000001";
    private static final String DEVICE_SN = "CR062R-TBOX-0001";
    private static final String PART_CODE = "CR062R_TBOX";
    private static final String CERT_SN = "CR062R-CERT-0001";
    private static final String RELEASE = "test-release-v1";
    private static final String DESCRIPTOR_SET_HASH = "test-descriptor-set-hash";

    @MockBean
    private KeyProvisioningTemplate keyProvisioningTemplate;
    @MockBean
    private CertResolver certResolver;
    @MockBean
    private CertificateEvidenceValidator certificateEvidenceValidator;

    @Autowired
    private KeyProvisioningApplicationFacade keyProvisioningApplicationFacade;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TestDeviceCertificate.Material material;

    @BeforeEach
    void setUp() throws Exception {
        clean();
        insertFixture();

        material = TestDeviceCertificate.generate();
        when(certResolver.resolve(any(DeviceCertificateRef.class), any()))
                .thenReturn(CertificateEvidenceValidator.fromCertificateDer(material.certificateDer(),
                        EvidenceSource.INLINE_DER));
        // 证书策略校验由 framework CR-016 单测覆盖；此处保留真实验签，仅以 inline DER 证据通过证据校验
        when(certificateEvidenceValidator.validate(any(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(0));

        when(keyProvisioningTemplate.deriveAndWrap(any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose purpose = inv.getArgument(2);
            net.hwyz.iov.cloud.framework.security.crypto.model.DerivationContext context = inv.getArgument(4);
            String operationId = inv.getArgument(5);
            return KeyprovEnvelopeTestFixtures.derivedWrappedKey("CR062R-KEY-" + purpose.name(), purpose,
                    context.businessKeyVersion(), 7L, "v1", operationId, material.fingerprintHex());
        });
    }

    private void clean() {
        jdbcTemplate.execute("DELETE FROM tb_business_key_provisioning_item WHERE provisioning_id IN "
                + "(SELECT id FROM tb_business_key_provisioning WHERE device_sn = '" + DEVICE_SN + "')");
        jdbcTemplate.execute("DELETE FROM tb_business_key_provisioning WHERE device_sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_device_business_key WHERE device_sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_veh_certificate WHERE device_sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_vehicle_part WHERE vin = '" + vin + "'");
        jdbcTemplate.execute("DELETE FROM tb_part_security_constant WHERE sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_part_info WHERE sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_veh_basic_info WHERE vin = '" + vin + "'");
    }

    private void insertFixture() {
        jdbcTemplate.update("INSERT INTO tb_veh_basic_info (vin, brand_code, platform_code, car_line_code, model_code, eol_time) "
                + "VALUES (?, 'BRAND', 'PLAT', 'SER', 'MODEL', NOW())", vin);
        jdbcTemplate.update("INSERT INTO tb_part_info (part_code, sn, instance_state, extra) VALUES (?, ?, 1, ?)",
                PART_CODE, DEVICE_SN, "{\"hsm\":\"" + HSM_UID + "\"}");
        jdbcTemplate.update("INSERT INTO tb_part_security_constant (part_code, sn, chip_uid, constant_type, preset_state) "
                + "VALUES (?, ?, ?, 'ROOT', 'PRESET')", PART_CODE, DEVICE_SN, HSM_UID);
        jdbcTemplate.update("INSERT INTO tb_vehicle_part (vin, part_id, bind_state) SELECT ?, id, 1 FROM tb_part_info WHERE sn = ?",
                vin, DEVICE_SN);
        jdbcTemplate.update("INSERT INTO tb_veh_certificate (request_id, cert_sn, vin, binding_id, part_id, device_category, "
                        + "device_sn, hsm_uid, certificate_profile, cert_status, not_after) "
                        + "SELECT ?, ?, ?, vp.id, pi.id, 'TBOX', ?, ?, 'TBOX_TSP_CLIENT', 'ACTIVE', DATE_ADD(NOW(), INTERVAL 365 DAY) "
                        + "FROM tb_vehicle_part vp JOIN tb_part_info pi ON pi.id = vp.part_id WHERE vp.vin = ? AND pi.sn = ?",
                "CR062R_CERT_REQ", CERT_SN, vin, DEVICE_SN, HSM_UID, vin, DEVICE_SN);
    }

    private KeyprovAuthenticatedContext ctx() {
        return new KeyprovAuthenticatedContext("VAGW_AUTHENTICATED_CONTEXT", HSM_UID, "client-1",
                CERT_SN, null, 1, "session-1", System.currentTimeMillis());
    }

    private String sha256(byte[] data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] signLowS(byte[] proofInput) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(material.keyPair().getPrivate());
        signature.update(proofInput);
        return EcdsaP1363.derToP1363(signature.sign());
    }

    private net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose frameworkPurpose(KeyPurpose purpose) {
        return purpose == KeyPurpose.KEY_PURPOSE_DOWNLINK_AEAD
                ? net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose.DOWNLINK_AEAD
                : net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose.UPLINK_AEAD;
    }

    @Test
    @DisplayName("真实 ECDSA P1363 验签通过 → 两用途原子 ACTIVE")
    void realVerifier_activatesBothPurposes() throws Exception {
        String requestId = "CR062R_REQ_001";
        KeyProvisioningRequest request = KeyProvisioningRequest.newBuilder()
                .setRequestId(requestId)
                .setIdempotencyKey("IK-" + requestId)
                .setBusinessDomain(DOMAIN)
                .addPurposes(KeyPurpose.KEY_PURPOSE_UPLINK_AEAD)
                .addPurposes(KeyPurpose.KEY_PURPOSE_DOWNLINK_AEAD)
                .addSupportedWrappingSuites(WrappingSuite.WRAPPING_SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM)
                .addSupportedEnvelopeFormats(KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1)
                .setTimestampMs(System.currentTimeMillis())
                .build();
        byte[] requestPayload = request.toByteArray();
        KeyProvisioningOperationResult requestResult = keyProvisioningApplicationFacade.request(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_REQUEST, "keyprov-v1", requestPayload, sha256(requestPayload),
                RELEASE, DESCRIPTOR_SET_HASH);
        assertEquals(VmdKeyProvisioningOutcome.SUCCESS, requestResult.getOutcome());
        KeyProvisioningResponse response = KeyProvisioningResponse.parseFrom(requestResult.getDownlinkPayload());

        KeyProvisioningAck.Builder ackBuilder = KeyProvisioningAck.newBuilder().setRequestId(requestId);
        for (var item : response.getItemsList()) {
            byte[] proofInput = ImportProofCanonical.proofInputV1(requestId, DOMAIN,
                    frameworkPurpose(item.getPurpose()), item.getKeyId(), item.getBusinessKeyVersion(),
                    item.getEnvelopeDigest().toByteArray(), item.getKeyCheckValue().toByteArray());
            ackBuilder.addItems(KeyImportItemResult.newBuilder()
                    .setPurpose(item.getPurpose())
                    .setKeyId(item.getKeyId())
                    .setBusinessKeyVersion(item.getBusinessKeyVersion())
                    .setResult(KeyImportResult.KEY_IMPORT_RESULT_IMPORTED)
                    .setImportProof(ByteString.copyFrom(signLowS(proofInput)))
                    .setProofAlgorithm("")
                    .setImportProofAlgorithm(net.hwyz.iov.security.keyprov.v1.ImportProofAlgorithm
                            .IMPORT_PROOF_ALGORITHM_ECDSA_P256_SHA256_P1363)
                    .setEnvelopeDigest(item.getEnvelopeDigest())
                    .setKeyCheckValue(item.getKeyCheckValue())
                    .setEnvelopeFormat(item.getEnvelopeFormat())
                    .setRecipientCertificateDigest(ByteString.copyFrom(material.certificateDigest()))
                    .build());
        }
        byte[] ackPayload = ackBuilder.build().toByteArray();
        KeyProvisioningOperationResult ackResult = keyProvisioningApplicationFacade.ack(CALLER, ctx(),
                KeyprovConstants.PAYLOAD_TYPE_ACK, "keyprov-v1", ackPayload, sha256(ackPayload));

        assertEquals(VmdKeyProvisioningOutcome.SUCCESS, ackResult.getOutcome(), "真实验签应通过");
        Integer active = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_device_business_key WHERE device_sn = ? AND business_domain = ? AND key_state = 'ACTIVE'",
                Integer.class, DEVICE_SN, DOMAIN);
        assertEquals(2, active, "两用途应原子激活");
    }
}
