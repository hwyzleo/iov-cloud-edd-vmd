package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSecurityConstantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client.KmsHsmClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client.dto.KmsHsmResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartSecurityPresetAppService {

    private final PartSecurityConstantRepository partSecurityConstantRepository;
    private final PartImportDataRepository partImportDataRepository;
    private final KmsHsmClient kmsHsmClient;

    private static final int DESCRIPTION_MAX_LENGTH = 500;
    private static final String SECURITY_CONSTANT_TYPE = "ROOT";

    @Transactional(rollbackFor = Exception.class)
    public void preset(String partCode, String sn, String chipUid, String batchNum) {
        log.info("开始预置零件[{}:{}]安全常量, chipUid={}, batchNum={}", partCode, sn, chipUid, batchNum);

        PartSecurityConstant existing = partSecurityConstantRepository.selectByPartCodeAndSn(partCode, sn);

        if (existing != null && existing.getPresetState() == SecurityConstantState.PRESET) {
            log.info("零件[{}:{}]安全常量已预置，跳过", partCode, sn);
            return;
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
            // chipUid 作为 KDF 派生绑定锚，确保常量与特定安全芯片绑定
            KmsHsmResult result = kmsHsmClient.generatePerDeviceConstant(partCode, sn, SECURITY_CONSTANT_TYPE, chipUid);

            securityConstant.setPresetState(SecurityConstantState.PRESET);
            securityConstant.setKmsKeyRef(result.getKmsKeyRef());
            securityConstant.setKeySpec(result.getKeySpec());
            securityConstant.setKmsProvider(result.getProvider());
            securityConstant.setAlgorithm(result.getAlgorithm());
            securityConstant.setGenTime(LocalDateTime.now());
            securityConstant.setLastAttemptTime(LocalDateTime.now());
            partSecurityConstantRepository.update(securityConstant);

            log.info("零件[{}:{}]安全常量预置成功", partCode, sn);
        } catch (Exception e) {
            handlePresetFailure(securityConstant, partCode, sn, batchNum, e.getMessage());
        }
    }

    private void handlePresetFailure(PartSecurityConstant securityConstant, String partCode, String sn, String batchNum, String errorMessage) {
        log.warn("零件[{}:{}]安全常量预置失败: {}", partCode, sn, errorMessage);

        try {
            securityConstant.setPresetState(SecurityConstantState.FAILED);
            securityConstant.setFailReason(truncateDescription(errorMessage));
            securityConstant.setLastAttemptTime(LocalDateTime.now());
            partSecurityConstantRepository.update(securityConstant);
        } catch (Exception e) {
            log.error("更新安全常量失败状态异常", e);
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
}