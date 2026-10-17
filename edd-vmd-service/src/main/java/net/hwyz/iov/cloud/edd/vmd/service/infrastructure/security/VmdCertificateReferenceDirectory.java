package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.util.CertificateSerialUtils;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificate;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleCertificateRepository;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateReferenceAmbiguousException;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateLocator;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateReferenceDirectory;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateSubjectRef;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.DeviceCertificateRef;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * VMD 薄证书目录 Adapter（VMD-DSN-CR-060 §4.1）。
 * <p>
 * 实现 framework {@link CertificateReferenceDirectory} SPI，把 VMD 已有的
 * {@code cert_sn → pki_request_id} 事实接入 framework 证书材料解析链：
 * <ol>
 *   <li>优先按 canonical serial 查询；命中唯一记录则返回 {@link CertificateLocator.EnrollmentRequest}；</li>
 *   <li>serial 未命中时，DEVICE/VEHICLE 主体按当前 active 绑定下唯一 ACTIVE 证书兜底；</li>
 *   <li>0 条命中返回 {@link Optional#empty()}；同 canonical serial 命中多条时抛
 *       {@link CertificateReferenceAmbiguousException}（不按更新时间取第一条）。</li>
 * </ol>
 * 落地约束：仅依赖 domain repository（framework 不直接访问 VMD 表）；locator 只返回 requestId，
 * 不返回 trusted/GOOD/DER/SPKI 或自行解析的 X.509 结论；缺失 {@code pkiRequestId} 视为无 locator
 * （fail-closed，由 framework 报证书材料不可取）。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VmdCertificateReferenceDirectory implements CertificateReferenceDirectory {

    /**
     * 设备身份证书 Profile（与 ActiveDeviceCertificateResolver 对齐）
     */
    private static final String DEVICE_IDENTITY_PROFILE = "TBOX_TSP_CLIENT";

    /**
     * 车辆主体兜底的设备类别
     */
    private static final String VEHICLE_DEVICE_CATEGORY = "TBOX";

    private final VehicleCertificateRepository vehicleCertificateRepository;

    @Override
    public Optional<CertificateLocator> locate(DeviceCertificateRef ref) {
        if (ref == null) {
            return Optional.empty();
        }
        String canonicalSerial = CertificateSerialUtils.canonical(ref.certSerial());
        if (canonicalSerial != null && !canonicalSerial.isBlank()) {
            List<VehicleCertificate> matches = vehicleCertificateRepository.selectByCanonicalCertSn(canonicalSerial);
            if (matches.isEmpty()) {
                log.warn("证书目录按 canonical serial 未命中: serial={}", canonicalSerial);
                return Optional.empty();
            }
            if (matches.size() > 1) {
                throw new CertificateReferenceAmbiguousException(
                        "证书目录同 canonical serial 命中多条记录: serial=" + canonicalSerial + ", count=" + matches.size());
            }
            return toLocator(matches.get(0));
        }
        CertificateSubjectRef subject = ref.subject();
        if (subject != null) {
            VehicleCertificate active = selectUniqueActiveBySubject(subject);
            if (active == null) {
                log.warn("证书目录按主体未命中唯一 ACTIVE 证书: subject={}", subject.value());
                return Optional.empty();
            }
            return toLocator(active);
        }
        return Optional.empty();
    }

    /**
     * 按主体解析当前 active 绑定下唯一 ACTIVE 证书（多于一条由 uk_device_profile_active 约束保证不出现）
     */
    private VehicleCertificate selectUniqueActiveBySubject(CertificateSubjectRef subject) {
        if (subject == null || subject.type() == null || subject.value() == null) {
            return null;
        }
        return switch (subject.type()) {
            case DEVICE -> vehicleCertificateRepository
                    .selectActiveByDeviceSnAndProfile(subject.value(), DEVICE_IDENTITY_PROFILE);
            case VEHICLE -> vehicleCertificateRepository
                    .selectActiveByVinAndDeviceCategory(subject.value(), VEHICLE_DEVICE_CATEGORY);
            default -> null;
        };
    }

    /**
     * 构造 locator：只返回 requestId；缺失视为无 locator（fail-closed）
     */
    private Optional<CertificateLocator> toLocator(VehicleCertificate cert) {
        String requestId = cert.getPkiRequestId();
        if (requestId == null || requestId.isBlank()) {
            log.warn("证书目录命中记录但缺少 pkiRequestId，视为无 locator: certSn={}", cert.getCertSn());
            return Optional.empty();
        }
        return Optional.of(new CertificateLocator.EnrollmentRequest(requestId));
    }
}
