package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security.CsrUtils;
import net.hwyz.iov.cloud.edd.vmd.service.support.TestTboxEcdhCsr;
import net.hwyz.iov.cloud.framework.security.crypto.DefaultCertEnrollmentTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.client.PkiClient;
import net.hwyz.iov.cloud.framework.security.crypto.enrollment.CertificateChainValidator;
import net.hwyz.iov.cloud.framework.security.crypto.enrollment.CertificateEnrollmentResultStore;
import net.hwyz.iov.cloud.framework.security.crypto.enrollment.EnrollmentIdempotencyService;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateProfileNotAllowedException;
import net.hwyz.iov.cloud.framework.security.crypto.metrics.CryptoMetrics;
import net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyRequest;
import net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult;
import net.hwyz.iov.cloud.framework.security.crypto.model.CertificateProfile;
import net.hwyz.iov.cloud.framework.security.crypto.model.EnrollmentState;
import net.hwyz.iov.cloud.framework.security.crypto.model.SubjectRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CR-063 框架 Profile Bean 注册回归测试
 * <p>
 * 背景：FW-SEC {@code CryptoAutoConfiguration.certEnrollmentTemplate(List<CertificateProfile>)}
 * 只把 Spring 上下文中 {@link CertificateProfile} 类型 Bean 收集为签发允许名单，框架自身仅注册
 * {@code TBOX_TSP_CLIENT}。若应用侧未注册 {@code TBOX_DEVICE_ECDH_ES_P256_V1}，
 * 重签/首次签发会在 {@code DefaultCertEnrollmentTemplate.validateProfile} 抛
 * {@code CertificateProfileNotAllowedException}（后台重签证书报错根因）。
 * <p>
 * 本测试使用真实 Spring 上下文中的 Profile Bean 列表 + 真实
 * {@link DefaultCertEnrollmentTemplate}（协作对象全部 Mock），完整复现并守护该回归：
 * 1) 上下文允许名单包含 ECDH-ES Profile 且字段与注册表 applyProfile 一致；
 * 2) 真实模板 allow-list 放行该 Profile（不允许名单缺失时必抛 NotAllowed）。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Rollback
@TestPropertySource(properties = "vmd.certificate.profiles.tbox-device-ecdh-es-p256-v1.enabled=true")
class CertificateProfileRegistrationTest extends BaseTest {

    private static final String V1_PROFILE = "TBOX_DEVICE_ECDH_ES_P256_V1";

    @Autowired
    private List<CertificateProfile> certificateProfiles;

    /**
     * 断言上下文允许名单包含 ECDH-ES Profile Bean，且取值与
     * {@code DefaultCertificateProfileRegistry} 的 applyProfile 一致。
     */
    @Test
    @DisplayName("框架签发允许名单包含 TBOX_DEVICE_ECDH_ES_P256_V1 Profile Bean")
    void ecdhEsProfileRegisteredInFrameworkAllowList() {
        CertificateProfile profile = certificateProfiles.stream()
                .filter(p -> V1_PROFILE.equals(p.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        V1_PROFILE + " 未注册为框架 CertificateProfile Bean（后台重签报错根因）"));
        assertEquals(V1_PROFILE, profile.pkiProfileId());
        assertEquals(CertificateProfile.SubjectType.DEVICE_IDENTITY, profile.subjectType());
        assertEquals("EC", profile.allowedAlgorithm());
        assertEquals("DIGITAL_SIGNATURE,KEY_AGREEMENT", profile.usage());
    }

    /**
     * 用真实 {@link DefaultCertEnrollmentTemplate} 复现后台重签路径：以 VMD
     * {@code CertificateProvisioningAppService.resolveApplyProfile} 产出的同名 Profile
     * 提交申请，断言允许名单放行（不会抛 CertificateProfileNotAllowedException）。
     * 签发后的 PKI 提交以 Mock 收尾，仅验证 allow-list 前置校验。
     */
    @Test
    @DisplayName("真实 DefaultCertEnrollmentTemplate 允许名单放行 ECDH-ES Profile 申请")
    void realEnrollmentTemplateAcceptsEcdhProfile() {
        CertificateProfile profile = certificateProfiles.stream()
                .filter(p -> V1_PROFILE.equals(p.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        V1_PROFILE + " 未注册为框架 CertificateProfile Bean（后台重签报错根因）"));

        PkiClient pkiClient = mock(PkiClient.class);
        CryptoMetrics cryptoMetrics = mock(CryptoMetrics.class);
        CertificateEnrollmentResultStore resultStore = mock(CertificateEnrollmentResultStore.class);
        EnrollmentIdempotencyService idempotencyService = mock(EnrollmentIdempotencyService.class);
        DefaultCertEnrollmentTemplate template = new DefaultCertEnrollmentTemplate(
                pkiClient, cryptoMetrics, certificateProfiles, resultStore, idempotencyService,
                new CertificateChainValidator());
        when(pkiClient.submit(any())).thenReturn(new PkiClient.ApplyResponse("pki-req-regression", "PENDING", null));

        CertApplyRequest request = new CertApplyRequest(
                profile,
                CsrUtils.decodeBase64(TestTboxEcdhCsr.valid("TBOX-UID-CR063-REGRESSION")),
                new SubjectRef(SubjectRef.SubjectType.DEVICE_UID, "TBOX-UID-CR063-REGRESSION"),
                "CR063_REGRESSION_REQ_001",
                null);

        CertApplyResult result = assertDoesNotThrowApply(template, request);
        assertNotNull(result);
        assertEquals(EnrollmentState.PENDING, result.state());
        assertEquals("pki-req-regression", result.requestId());
    }

    private CertApplyResult assertDoesNotThrowApply(DefaultCertEnrollmentTemplate template, CertApplyRequest request) {
        try {
            return template.apply(request);
        } catch (CertificateProfileNotAllowedException e) {
            throw new AssertionError(
                    "允许名单缺失: " + V1_PROFILE + "（后台重签报错根因复现）", e);
        }
    }
}
