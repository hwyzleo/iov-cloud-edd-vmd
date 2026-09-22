package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import org.springframework.stereotype.Service;

/**
 * HSM UID 字段解析器（CR-049 §4.2）
 * <p>
 * 本期 HSM UID 导入字段名统一默认 HSM（RD-049-3），实现只保留单一常量默认值；
 * 未来若 MDM 增列 hsmUidField，可直接接入本 Resolver，不再修改节点枚举。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-09-22
 */
@Service
public class HsmUidFieldResolver {

    /**
     * 默认 HSM UID 字段名
     */
    public static final String DEFAULT_HSM_UID_FIELD = "HSM";

    /**
     * 解析 HSM UID 字段名
     *
     * @param configuredField MDM 配置字段名（本期无，传 null）
     * @return 生效字段名，默认 HSM
     */
    public String resolve(String configuredField) {
        return (configuredField != null && !configuredField.isBlank())
                ? configuredField
                : DEFAULT_HSM_UID_FIELD;
    }
}
