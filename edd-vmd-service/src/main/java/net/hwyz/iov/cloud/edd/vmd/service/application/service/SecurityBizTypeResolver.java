package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.SecurityPresetBizTypeUnresolvedException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleNodeSchemaRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.SecurityPresetMetrics;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

/**
 * 安全常量预置 BizType 解析器（CR-049 §4.3）
 * <p>
 * BizType 仍由 VMD 代码控制以保护 KMS key 域边界（R-049-4）：
 * 路由主键从具体节点码调整为稳定的 deviceCategory，新增同类节点变体无需发版。
 * 路由顺序：deviceCategory 受控映射 → 无类别/无映射时旧节点码兜底（warn + 指标）→
 * 仍不可解析抛出 {@link SecurityPresetBizTypeUnresolvedException}（RD-049-5）。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-09-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityBizTypeResolver {

    private final VehicleNodeSchemaRegistry legacyRegistry;
    private final SecurityPresetMetrics metrics;

    /**
     * 受控 deviceCategory → BizType 映射（对应既有 BizType 与 KMS key 配置，不在本 CR 新建密钥域）
     * <p>
     * 智驾域控（DCU_ADAS_GEN1 等）路由至既有 AD_DCU_DEVICE_ROOT 密钥域；
     * 兼容 framework DeviceCategory 命名（AD_DCU）与 VMD/MDM 节点前缀命名（DCU_ADAS）两种取值。
     * </p>
     */
    private static final Map<String, BizType> CATEGORY_BIZ_TYPE = Map.of(
            "TBOX", BizType.TBOX_DEVICE_ROOT,
            "CCU", BizType.CCU_DEVICE_ROOT,
            "BTM", BizType.PEPS_DEVICE_ROOT,
            "CGW", BizType.CGW_DEVICE_ROOT,
            "DCU_COCKPIT", BizType.CPT_DCU_DEVICE_ROOT,
            "DCU_ADAS", BizType.AD_DCU_DEVICE_ROOT,
            "AD_DCU", BizType.AD_DCU_DEVICE_ROOT
    );

    /**
     * 歧义设备族：DCU（域控制器大类）同时覆盖座舱域控与智驾域控，
     * 需按 MDM VehicleNode.funcDomain 消歧，避免跨 KMS key 域误路由（R-049-4）。
     * 其余功能域（GENERAL/CONNECTIVITY 等）不落入受控消歧，回落旧节点码兜底。
     */
    private static final Map<String, BizType> DCU_FUNC_DOMAIN_BIZ_TYPE = Map.of(
            "ADAS", BizType.AD_DCU_DEVICE_ROOT,
            "COCKPIT", BizType.CPT_DCU_DEVICE_ROOT
    );

    /**
     * 解析器件级安全常量预置的 BizType
     *
     * @param deviceCategory 设备类别（MDM VehicleNode.deviceCategory，可空）
     * @param funcDomain     功能域（MDM VehicleNode.funcDomain，可空；用于 DCU 歧义消歧）
     * @param legacyNodeCode 车载节点代码（迁移期兜底）
     * @return BizType
     * @throws SecurityPresetBizTypeUnresolvedException 需预置但最终仍无法解析 BizType
     */
    public BizType resolve(String deviceCategory, String funcDomain, String legacyNodeCode) {
        if (deviceCategory != null && !deviceCategory.isBlank()) {
            String category = deviceCategory.trim().toUpperCase(Locale.ROOT);
            if ("DCU".equals(category)) {
                // 歧义设备族：按功能域消歧（ADAS→智驾域，COCKPIT→座舱域）
                BizType byDomain = resolveDcuByFuncDomain(funcDomain);
                if (byDomain != null) {
                    log.info("BizType 按 deviceCategory=DCU+funcDomain 路由: deviceCategory={}, funcDomain={}, nodeCode={}, bizType={}",
                            deviceCategory, funcDomain, legacyNodeCode, byDomain);
                    return byDomain;
                }
                log.warn("deviceCategory=DCU 但 funcDomain[{}]不可消歧，落旧节点码兜底: nodeCode={}",
                        funcDomain, legacyNodeCode);
                metrics.recordBizTypeUnresolved(deviceCategory, legacyNodeCode);
            } else {
                BizType bizType = CATEGORY_BIZ_TYPE.get(category);
                if (bizType != null) {
                    log.info("BizType 按 deviceCategory 路由: deviceCategory={}, nodeCode={}, bizType={}",
                            deviceCategory, legacyNodeCode, bizType);
                    return bizType;
                }
                metrics.recordBizTypeUnresolved(deviceCategory, legacyNodeCode);
            }
        }

        // 无类别或无受控映射 → 迁移期按旧节点码兜底
        BizType legacy = legacyRegistry.getBizType(legacyNodeCode);
        if (legacy != null) {
            log.warn("deviceCategory[{}] 无受控映射，按旧节点码兜底: nodeCode={}, bizType={}",
                    deviceCategory, legacyNodeCode, legacy);
            return legacy;
        }

        metrics.recordBizTypeUnresolved(deviceCategory, legacyNodeCode);
        log.error("需要安全常量预置但 BizType 不可解析: deviceCategory={}, nodeCode={}",
                deviceCategory, legacyNodeCode);
        throw new SecurityPresetBizTypeUnresolvedException(deviceCategory, legacyNodeCode);
    }

    /**
     * DCU 歧义设备族按功能域消歧
     *
     * @param funcDomain 功能域（可空）
     * @return 消歧后的 BizType；功能域缺失或不可识别返回 null
     */
    private BizType resolveDcuByFuncDomain(String funcDomain) {
        if (funcDomain == null || funcDomain.isBlank()) {
            return null;
        }
        return DCU_FUNC_DOMAIN_BIZ_TYPE.get(funcDomain.trim().toUpperCase(Locale.ROOT));
    }
}
