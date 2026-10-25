package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKey;
import net.hwyz.iov.security.keyprov.v1.KeyEnvelopeFormat;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningRequest;
import net.hwyz.iov.security.keyprov.v1.WrappedKeyItem;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * VMD keyprov 协议 Adapter：Request envelope 能力协商 + 委托 PAR-PROTO Wire Mapper 组装 Response item
 * （VMD-DSN-CR-068 / PAR-PROTO-DSN-CR-009）。
 * <p>
 * 职责：
 * <ul>
 *   <li>Request：对 {@code supported_wrapping_suites}/{@code supported_envelope_formats} 去重校验，
 *       仅当显式声明并命中 ECDH-ES + {@code ECDH_ES_P256_V1} allowlist 时才允许 envelope v1，否则
 *       不下发、不降级（缺失/UNSPECIFIED/未知关键值 fail-closed）；</li>
 *   <li>Response：委托 {@link ParProtoWireMapperV1}——校验 view/coreEnvelopeDigest、构建 deterministic
 *       parameters、断言 canonical AAD 一致性、计算 9 字段 wireEnvelopeDigest 并组装 {@link WrappedKeyItem}
 *       （envelope_digest 为 wire 摘要）；core digest 不得进入 wire。</li>
 * </ul>
 *
 * @author hwyz_leo
 * @since 2026-10-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeyprovEnvelopeAdapter {

    private final ParProtoWireMapperV1 wireMapper;

    /**
     * Request envelope v1 能力协商（fail-closed）。
     *
     * @param request canonical KeyProvisioningRequest
     * @return 命中 allowlist 返回 {@link KeyEnvelopeFormat#KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1}，否则 {@code null}
     */
    public KeyEnvelopeFormat negotiateEnvelopeFormat(KeyProvisioningRequest request) {
        if (request == null) {
            return null;
        }
        Set<KeyEnvelopeFormat> formats = new LinkedHashSet<>();
        for (KeyEnvelopeFormat format : request.getSupportedEnvelopeFormatsList()) {
            if (format == null || format == KeyEnvelopeFormat.UNRECOGNIZED
                    || format == KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_UNSPECIFIED) {
                // 未知/UNSPECIFIED 关键值 fail-closed，不做前缀或别名推断
                return null;
            }
            formats.add(format);
        }
        if (!formats.contains(KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1)) {
            return null;
        }
        boolean ecdhDeclared = request.getSupportedWrappingSuitesList().stream()
                .anyMatch(suite -> suite == net.hwyz.iov.security.keyprov.v1.WrappingSuite
                        .WRAPPING_SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM);
        if (!ecdhDeclared) {
            return null;
        }
        return KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1;
    }

    /**
     * 将派生封装结果映射为 canonical {@link WrappedKeyItem}（委托 PAR-PROTO Wire Mapper）。
     *
     * @param requestId     wire 上下文 requestId
     * @param businessDomain wire 上下文业务域（V2C_COMM）
     * @param directory     目标业务密钥目录行（keyAlgorithm/有效期）
     * @param source        framework 根派生封装结果
     * @return canonical WrappedKeyItem（envelope_digest=wire digest）
     */
    public WrappedKeyItem toWrappedKeyItem(String requestId, String businessDomain, DeviceBusinessKey directory,
                                           DerivedWrappedKey source) {
        return wireMapper.map(source, new KeyProvisioningWireContext(requestId, businessDomain), directory).item();
    }
}
