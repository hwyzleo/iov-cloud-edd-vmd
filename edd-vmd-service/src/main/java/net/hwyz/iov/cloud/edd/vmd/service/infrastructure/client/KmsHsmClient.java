package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client.dto.KmsHsmResult;

/**
 * KMS/HSM 客户端接口
 * <p>
 * 对接安全与合规域 KMS/HSM 服务，生成安全常量。
 * VMD 不自行产生/保管密钥，所有密钥在 HSM 内生成、明文不出件。
 *
 * @author hwyz_leo
 * @since 2026-06-24
 */
public interface KmsHsmClient {

    /**
     * 生成 per-VIN 安全常量
     * <p>
     * 按车架号派生安全常量，用于车辆级安全通信根密钥。
     *
     * @param vin 车架号
     * @return KMS 结果（包含密钥引用、密钥规格等）
     * @throws Exception KMS/HSM 服务调用异常
     */
    KmsHsmResult generatePerVinConstant(String vin) throws Exception;

    /**
     * 生成器件级安全常量
     * <p>
     * 按器件 (partCode, sn) 派生安全常量，chipUid 作为 KDF 派生绑定锚。
     * 常量在 HSM 内生成，明文不出件；VMD 仅持久化 KMS 引用。
     *
     * @param partCode     零件编码
     * @param sn           零件序列号
     * @param constantType 安全常量类型（ROOT/SECOC/IMMO/SEED_KEY）
     * @param chipUid      安全芯片/HSM 唯一标识（KDF 派生绑定锚）
     * @return KMS 结果（包含密钥引用、密钥规格等）
     * @throws Exception KMS/HSM 服务调用异常
     */
    KmsHsmResult generatePerDeviceConstant(String partCode, String sn, String constantType, String chipUid) throws Exception;
}
