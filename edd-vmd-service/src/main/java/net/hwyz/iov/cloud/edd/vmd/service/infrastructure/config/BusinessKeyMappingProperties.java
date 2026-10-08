package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

/**
 * 业务密钥目录上下文映射配置（CR-055 §7.2）
 * <p>
 * framework 运行时目录解析传入 {@code DeviceKeyContext(bizType)}；VMD 目录以
 * (device_sn, business_domain, purpose) 为键。本配置将 framework BizType 名映射到
 * VMD 受治理业务域代码，未配置时默认取 BizType 名本身（identity）。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "vmd.business-key.mapping")
public class BusinessKeyMappingProperties {

    /**
     * BizType 名 → 业务域代码；缺省时业务域 = BizType 名
     */
    private Map<String, String> bizTypeDomain = new HashMap<>();

    /**
     * 业务域代码 → BizType 名（framework create 分类标签需要）；
     * 缺省时若业务域恰为 BizType 名则直接使用，否则 fail-closed
     */
    private Map<String, String> domainBizType = new HashMap<>();
}
