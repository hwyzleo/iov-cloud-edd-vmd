package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDerivationRejectedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDerivedProviderUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyKmsUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyMaterialTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyMaterialModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 业务密钥材料模型路由（VMD-DSN-CR-061 RD-061-3 / RD-061-10）。
 * <p>
 * 按 {@code BizType.materialModel()} <strong>显式</strong>路由，禁止依据方法调用动态猜测，禁止隐式回退：
 * <ul>
 *   <li>{@link KeyMaterialModel#DERIVED_ROOT} → {@link KeyProvisioningTemplate#deriveAndWrap}</li>
 *   <li>{@link KeyMaterialModel#RANDOM_MATERIAL} → {@link BusinessKeyMaterialTemplate#create}/{@code wrap}/{@code revoke}</li>
 *   <li>{@code null}（不适用，如非对称签名条目）→ 两条路径均拒绝</li>
 * </ul>
 * V2C（{@code V2C_COMM_ROOT}）固定走根派生；{@code wrapBy*} 等兼容门面即使存在也不得作为 V2C 入口。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessKeyMaterialRouter {

    private final ObjectProvider<KeyProvisioningTemplate> derivedRootProvider;
    private final ObjectProvider<BusinessKeyMaterialTemplate> randomMaterialProvider;

    /**
     * 解析 BizType 的材料模型（可能为 {@code null}，表示不适用）。
     *
     * @param bizType framework 业务类型
     * @return 材料模型或 {@code null}
     */
    public KeyMaterialModel materialModel(BizType bizType) {
        Objects.requireNonNull(bizType, "bizType must not be null");
        return bizType.materialModel();
    }

    /**
     * 是否为根派生型（{@code prov != null && materialModel == DERIVED_ROOT}）。
     */
    public boolean isDerivedRoot(BizType bizType) {
        return bizType != null && bizType.supportsDerivedRoot();
    }

    /**
     * 是否为独立随机材料型（{@code materialModel == RANDOM_MATERIAL}）。
     */
    public boolean isRandomMaterial(BizType bizType) {
        return bizType != null && bizType.supportsRandomMaterial();
    }

    /**
     * 取根派生门面；非 DERIVED_ROOT 一律拒绝（fail-closed，禁隐式回退）。
     *
     * @param bizType framework 业务类型
     * @return {@link KeyProvisioningTemplate}
     * @throws BusinessKeyDerivationRejectedException      材料模型不是 DERIVED_ROOT
     * @throws BusinessKeyDerivedProviderUnavailableException 根派生门面未装配
     */
    public KeyProvisioningTemplate requireDerivedRoot(BizType bizType) {
        Objects.requireNonNull(bizType, "bizType must not be null");
        return requireDerivedRoot(bizType.materialModel(), bizType.supportsDerivedRoot(), bizType.name());
    }

    /**
     * 根派生路由核心（package-private，供单测以材料模型直接驱动；framework 当前无 RANDOM_MATERIAL 条目，
     * {@link BizType} 为 final 枚举不可构造替身）。
     *
     * @param model                材料模型（可为 {@code null} 表示不适用）
     * @param supportsDerivedRoot  是否声明具备根派生能力（{@code materialModel==DERIVED_ROOT && prov!=null}）
     * @param label                用于日志/异常的 BizType 名称
     */
    KeyProvisioningTemplate requireDerivedRoot(KeyMaterialModel model, boolean supportsDerivedRoot, String label) {
        if (!supportsDerivedRoot) {
            throw new BusinessKeyDerivationRejectedException(
                    "bizType 不是根派生型（materialModel=" + model + "）: " + label);
        }
        KeyProvisioningTemplate template = derivedRootProvider.getIfAvailable();
        if (template == null) {
            throw new BusinessKeyDerivedProviderUnavailableException(
                    "framework 根派生门面未装配（crypto.provisioning.enabled=false）");
        }
        return template;
    }

    /**
     * 取随机材料门面；非 RANDOM_MATERIAL 一律拒绝（fail-closed，禁隐式回退）。
     *
     * @param bizType framework 业务类型
     * @return {@link BusinessKeyMaterialTemplate}
     * @throws BusinessKeyDerivationRejectedException 材料模型不是 RANDOM_MATERIAL
     * @throws BusinessKeyKmsUnavailableException     随机材料门面未装配
     */
    public BusinessKeyMaterialTemplate requireRandomMaterial(BizType bizType) {
        Objects.requireNonNull(bizType, "bizType must not be null");
        return requireRandomMaterial(bizType.materialModel(), bizType.supportsRandomMaterial(), bizType.name());
    }

    /**
     * 随机材料路由核心（package-private，供单测以材料模型直接驱动）。
     *
     * @param model                 材料模型（可为 {@code null} 表示不适用）
     * @param supportsRandomMaterial 是否声明为独立随机材料型（{@code materialModel==RANDOM_MATERIAL}）
     * @param label                 用于日志/异常的 BizType 名称
     */
    BusinessKeyMaterialTemplate requireRandomMaterial(KeyMaterialModel model, boolean supportsRandomMaterial,
                                                      String label) {
        if (!supportsRandomMaterial) {
            throw new BusinessKeyDerivationRejectedException(
                    "bizType 不是独立随机材料型（materialModel=" + model + "）: " + label);
        }
        BusinessKeyMaterialTemplate template = randomMaterialProvider.getIfAvailable();
        if (template == null) {
            throw new BusinessKeyKmsUnavailableException(
                    "framework 随机材料门面未装配（crypto.business-key.enabled=false）");
        }
        return template;
    }

    /**
     * 是否装配了根派生门面（供 Health / 诊断使用，不触发拒绝）。
     */
    public boolean derivedRootAvailable() {
        return derivedRootProvider.getIfAvailable() != null;
    }

    /**
     * 是否装配了随机材料门面。
     */
    public boolean randomMaterialAvailable() {
        return randomMaterialProvider.getIfAvailable() != null;
    }
}