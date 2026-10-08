package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config;

import lombok.Data;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyAction;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 业务密钥授权策略配置（CR-055 §3.3）
 * <p>
 * 版本化 BusinessKeyPolicyRegistry 的数据来源。默认无任何条目（fail-closed）：
 * 未在配置中登记的 (business_domain, purpose) 一律拒绝授权。
 * 生产环境通过 Nacos 等配置中心下发受治理条目。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "vmd.business-key.policy")
public class BusinessKeyPolicyProperties {

    /**
     * 策略版本号（创建时写入 policy_version 快照）
     */
    private String version = "v1";

    /**
     * 受治理策略条目
     */
    private List<Entry> entries = new ArrayList<>();

    @Data
    public static class Entry {

        /**
         * 受治理业务域代码
         */
        private String businessDomain;

        /**
         * 受治理用途代码
         */
        private String purpose;

        /**
         * 允许的设备类别（如 TBOX；空表示不限制）
         */
        private List<String> deviceCategories = new ArrayList<>();

        /**
         * 允许的操作（PROVISION/RESOLVE_ACTIVE/DECRYPT/ROTATE/REVOKE）
         */
        private List<BusinessKeyAction> allowedActions = new ArrayList<>();

        /**
         * 密钥算法（透传 KMS）
         */
        private String algorithm;

        /**
         * 密钥规格
         */
        private String keySpec;

        /**
         * 密钥有效期
         */
        private Duration validity;

        /**
         * DEPRECATED 仅解密重叠窗口
         */
        private Duration decryptWindow;

        /**
         * 该域/用途在框架运行时目录解析（purpose 为空）时使用的默认用途
         */
        private String defaultPurpose;
    }
}
