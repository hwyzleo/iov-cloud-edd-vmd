package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client.impl;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client.KmsHsmClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client.dto.KmsHsmResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * KMS/HSM 客户端模拟实现
 * <p>
 * 用于开发和测试环境，模拟密钥生成行为。
 * 通过配置 kms.hsm.type=mock 启用。
 *
 * @author hwyz_leo
 * @since 2026-06-25
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "kms.hsm.type", havingValue = "mock", matchIfMissing = true)
public class MockKmsHsmClient implements KmsHsmClient {

    @Override
    public KmsHsmResult generatePerVinConstant(String vin) {
        String mockConstant = "vault:v1:bW9jay1wZXItaXYtY29uc3RhbnQtdmFsdWU=";
        log.info("[MOCK] 生成 per-VIN 安全常量, vin={}", vin);
        return KmsHsmResult.builder()
                .kmsKeyRef(mockConstant)
                .keySpec("aes256-gcm96")
                .provider("Mock")
                .algorithm("HMAC-SHA256")
                .build();
    }

    @Override
    public KmsHsmResult generatePerDeviceConstant(String partCode, String sn, String constantType, String chipUid) {
        String mockConstant = "vault:v1:bW9jay1wZXItZGV2aWNlLWNvbnN0YW50LXZhbHVl";
        log.info("[MOCK] 生成器件级安全常量, partCode={}, sn={}, constantType={}, chipUid={}", partCode, sn, constantType, chipUid);
        return KmsHsmResult.builder()
                .kmsKeyRef(mockConstant)
                .keySpec("aes256-gcm96")
                .provider("Mock")
                .algorithm("HMAC-SHA256")
                .build();
    }
}
