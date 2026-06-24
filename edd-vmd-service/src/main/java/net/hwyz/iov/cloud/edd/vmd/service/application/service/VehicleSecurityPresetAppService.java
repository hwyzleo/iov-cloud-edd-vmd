package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehSecurityConstantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 车辆安全常量预置应用服务
 *
 * @author hwyz_leo
 * @since 2026-06-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleSecurityPresetAppService {

    private final VehSecurityConstantRepository vehSecurityConstantRepository;
    private final VehImportDataRepository vehImportDataRepository;

    /**
     * description 字段最大长度（与数据库列定义一致）
     */
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    /**
     * 安全常量类型
     */
    private static final String SECURITY_CONSTANT_TYPE = "ROOT";

    /**
     * 预置per-VIN安全常量
     *
     * @param vin 车架号
     * @param batchNum 批次号
     */
    @Transactional(rollbackFor = Exception.class)
    public void preset(String vin, String batchNum) {
        log.info("开始预置车辆[{}]安全常量, batchNum={}", vin, batchNum);

        // 查询是否已存在记录
        VehSecurityConstant existing = vehSecurityConstantRepository.selectByVin(vin);

        // 幂等检查：如果已存在且状态为PRESET，跳过
        if (existing != null && existing.getPresetState() == SecurityConstantState.PRESET) {
            log.info("车辆[{}]安全常量已预置，跳过", vin);
            return;
        }

        // 创建或更新记录
        VehSecurityConstant securityConstant;
        if (existing == null) {
            securityConstant = VehSecurityConstant.builder()
                    .vin(vin)
                    .batchNum(batchNum)
                    .presetState(SecurityConstantState.PENDING)
                    .constantType(SECURITY_CONSTANT_TYPE)
                    .createTime(LocalDateTime.now())
                    .build();
            securityConstant.init();
            vehSecurityConstantRepository.insert(securityConstant);
        } else {
            securityConstant = existing;
            securityConstant.setPresetState(SecurityConstantState.PENDING);
            securityConstant.setBatchNum(batchNum);
        }

        try {
            // TODO: 集成KMS/HSM客户端
            String kmsKeyRef = "mock_kms_key_ref_" + vin;

            securityConstant.setPresetState(SecurityConstantState.PRESET);
            securityConstant.setKmsKeyRef(kmsKeyRef);
            securityConstant.setGenTime(LocalDateTime.now());
            securityConstant.setLastAttemptTime(LocalDateTime.now());
            vehSecurityConstantRepository.update(securityConstant);

            log.info("车辆[{}]安全常量预置成功", vin);
        } catch (Exception e) {
            handlePresetFailure(securityConstant, vin, batchNum, e.getMessage());
        }
    }

    /**
     * 处理预置失败
     */
    private void handlePresetFailure(VehSecurityConstant securityConstant, String vin, String batchNum, String errorMessage) {
        log.warn("车辆[{}]安全常量预置失败: {}", vin, errorMessage);

        // 更新状态为FAILED
        try {
            securityConstant.setPresetState(SecurityConstantState.FAILED);
            securityConstant.setFailReason(truncateDescription(errorMessage));
            securityConstant.setLastAttemptTime(LocalDateTime.now());
            vehSecurityConstantRepository.update(securityConstant);
        } catch (Exception e) {
            log.error("更新安全常量失败状态异常", e);
        }

        // 写回veh_import_data.description
        try {
            VehImportData vehImportData = vehImportDataRepository.selectByBatchNum(batchNum);
            if (vehImportData != null) {
                String description = vehImportData.getDescription();
                String newDescription = "安全常量预置失败: " + errorMessage;
                if (description != null) {
                    newDescription = description + "; " + newDescription;
                }
                vehImportData.setDescription(truncateDescription(newDescription));
                vehImportDataRepository.update(vehImportData);
            }
        } catch (Exception e) {
            log.error("写回veh_import_data.description失败", e);
        }
    }

    /**
     * 截断 description 到列长限制
     *
     * @param description 原始描述
     * @return 截断后的描述
     */
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
