package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSecurityConstantRepository;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.ProvisioningResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartSecurityPresetAppService {

    private final PartSecurityConstantRepository partSecurityConstantRepository;
    private final PartImportDataRepository partImportDataRepository;
    private final KeyProvisioningTemplate keyProvisioningTemplate;

    private static final int DESCRIPTION_MAX_LENGTH = 500;
    private static final String SECURITY_CONSTANT_TYPE = "ROOT";

    /**
     * 正常导入链路的安全常量预置（含写回 part_import_data.description 的副作用）
     */
    @Transactional(rollbackFor = Exception.class)
    public String preset(String partCode, String sn, String chipUid, String batchNum, String vehicleNodeCode, BizType bizType) {
        return doPreset(partCode, sn, chipUid, batchNum, vehicleNodeCode, bizType, true);
    }

    /**
     * 零件导入后置处理重放的安全常量补偿入口（VMD-DSN-CR-056）
     * <p>
     * 与 {@link #preset(String, String, String, String, String, BizType)} 复用同一 KMS 派生与
     * part_security_constant 状态推进，但<b>不写回 part_import_data.description</b>，
     * 遵守重放编排「不修改原导入记录状态」的边界（D34 / CR-056 §7）。
     * 幂等键 (partCode, sn, constantType)：PRESET 即视为已完成，缺失或 FAILED 允许补偿。
     *
     * @param partCode        零件编码
     * @param sn              零件序列号
     * @param chipUid         安全芯片 UID（源自当前 part_info.extra.HSM）
     * @param batchNum        原导入批次号（仅溯源）
     * @param vehicleNodeCode 车载节点代码
     * @param bizType         器件级安全常量 BizType
     * @return null 表示成功或已预置；非 null 为失败原因（已落 part_security_constant.fail_reason）
     */
    public String presetForReplay(String partCode, String sn, String chipUid, String batchNum, String vehicleNodeCode, BizType bizType) {
        return doPreset(partCode, sn, chipUid, batchNum, vehicleNodeCode, bizType, false);
    }

    /**
     * 安全常量预置核心编排
     *
     * @param updateImportDescription 是否写回 part_import_data.description（正常导入 true / 重放补偿 false）
     */
    private String doPreset(String partCode, String sn, String chipUid, String batchNum,
                            String vehicleNodeCode, BizType bizType, boolean updateImportDescription) {
        log.info("开始预置零件[{}:{}]安全常量, chipUid={}, batchNum={}, vehicleNodeCode={}, bizType={}, updateImportDescription={}",
                partCode, sn, chipUid, batchNum, vehicleNodeCode, bizType, updateImportDescription);

        PartSecurityConstant existing = partSecurityConstantRepository.selectByPartCodeAndSn(partCode, sn);

        if (existing != null && existing.getPresetState() == SecurityConstantState.PRESET) {
            log.info("零件[{}:{}]安全常量已预置，跳过", partCode, sn);
            return null;
        }

        if (bizType == null) {
            throw new IllegalArgumentException("安全常量预置 BizType 为空: partCode=" + partCode + ", vehicleNodeCode=" + vehicleNodeCode);
        }

        PartSecurityConstant securityConstant;
        if (existing == null) {
            securityConstant = PartSecurityConstant.builder()
                    .partCode(partCode)
                    .sn(sn)
                    .chipUid(chipUid)
                    .presetState(SecurityConstantState.PENDING)
                    .constantType(SECURITY_CONSTANT_TYPE)
                    .batchNum(batchNum)
                    .createTime(LocalDateTime.now())
                    .build();
            securityConstant.init();
            partSecurityConstantRepository.insert(securityConstant);
        } else {
            securityConstant = existing;
            securityConstant.setPresetState(SecurityConstantState.PENDING);
            securityConstant.setChipUid(chipUid);
            securityConstant.setBatchNum(batchNum);
        }

        try {
            ProvisioningResult result = keyProvisioningTemplate.deriveByUid(chipUid, bizType);

            securityConstant.setPresetState(SecurityConstantState.PRESET);
            securityConstant.setKmsKeyRef(result.getKmsKeyRef());
            securityConstant.setKeySpec(result.getKeySpec());
            securityConstant.setKmsProvider(result.getProvider());
            securityConstant.setAlgorithm(result.getAlgorithm());
            securityConstant.setKcv(bytesToHex(result.getKcv()));
            securityConstant.setGenTime(LocalDateTime.now());
            securityConstant.setLastAttemptTime(LocalDateTime.now());
            partSecurityConstantRepository.update(securityConstant);

            log.info("零件[{}:{}]安全常量预置成功", partCode, sn);
            return null;
        } catch (Exception e) {
            handlePresetFailure(securityConstant, partCode, sn, batchNum, e.getMessage(), updateImportDescription);
            // 返回失败信息（KMS异常详情已写入 fail_reason 与可选导入备注），供调用方计入失败
            return "安全常量预置失败: " + e.getMessage();
        }
    }

    private void handlePresetFailure(PartSecurityConstant securityConstant, String partCode, String sn,
                                     String batchNum, String errorMessage, boolean updateImportDescription) {
        log.warn("零件[{}:{}]安全常量预置失败: {}", partCode, sn, errorMessage);

        try {
            securityConstant.setPresetState(SecurityConstantState.FAILED);
            securityConstant.setFailReason(truncateDescription(errorMessage));
            securityConstant.setLastAttemptTime(LocalDateTime.now());
            partSecurityConstantRepository.update(securityConstant);
        } catch (Exception e) {
            log.error("更新安全常量失败状态异常", e);
        }

        // 仅正常导入链路写回 part_import_data.description；重放补偿不修改原导入记录（CR-056）
        if (!updateImportDescription) {
            return;
        }

        try {
            PartImportData partImportData = partImportDataRepository.selectByBatchNum(batchNum);
            if (partImportData != null) {
                String description = partImportData.getDescription();
                String newDescription = "安全常量预置失败: " + errorMessage;
                if (description != null) {
                    newDescription = description + "; " + newDescription;
                }
                partImportData.setDescription(truncateDescription(newDescription));
                partImportDataRepository.update(partImportData);
            }
        } catch (Exception e) {
            log.error("写回part_import_data.description失败", e);
        }
    }

    private String truncateDescription(String description) {
        if (description == null) {
            return null;
        }
        if (description.length() <= DESCRIPTION_MAX_LENGTH) {
            return description;
        }
        return description.substring(0, DESCRIPTION_MAX_LENGTH - 3) + "...";
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
