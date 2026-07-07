package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehSecurityConstantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.ProvisioningResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 车辆车云通信根预置应用服务
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
    private final KeyProvisioningTemplate keyProvisioningTemplate;

    /**
     * description 字段最大长度（与数据库列定义一致）
     */
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    /**
     * 安全常量类型
     */
    private static final String SECURITY_CONSTANT_TYPE = "ROOT";

    /**
     * 预置车云通信根
     *
     * @param vin 车架号
     * @param batchNum 批次号
     */
    @Transactional(rollbackFor = Exception.class)
    public void preset(String vin, String batchNum) {
        log.info("开始预置车辆[{}]车云通信根, batchNum={}", vin, batchNum);

        // EOL 补发的 VehicleProduceEvent 不触发安全预置
        // 仅真实 PRODUCE 批次触发
        if (batchNum != null && batchNum.startsWith("EOL-")) {
            log.debug("EOL 补发的 PRODUCE 事件，不触发安全预置: {}", vin);
            return;
        }

        // 查询是否已存在记录
        VehSecurityConstant existing = vehSecurityConstantRepository.selectByVin(vin);

        // 幂等检查：如果已存在且状态为PRESET，跳过
        if (existing != null && existing.getPresetState() == SecurityConstantState.PRESET) {
            log.info("车辆[{}]车云通信根已预置，跳过", vin);
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
            ProvisioningResult result = keyProvisioningTemplate.deriveByVin(vin, BizType.V2C_COMM_ROOT);

            securityConstant.setPresetState(SecurityConstantState.PRESET);
            securityConstant.setKmsKeyRef(result.getKmsKeyRef());
            securityConstant.setKeySpec(result.getKeySpec());
            securityConstant.setKmsProvider(result.getProvider());
            securityConstant.setAlgorithm(result.getAlgorithm());
            securityConstant.setKcv(bytesToHex(result.getKcv()));
            securityConstant.setGenTime(LocalDateTime.now());
            securityConstant.setLastAttemptTime(LocalDateTime.now());
            vehSecurityConstantRepository.update(securityConstant);

            log.info("车辆[{}]车云通信根预置成功", vin);
        } catch (Exception e) {
            handlePresetFailure(securityConstant, vin, batchNum, e.getMessage());
        }
    }

    /**
     * 处理预置失败
     */
    private void handlePresetFailure(VehSecurityConstant securityConstant, String vin, String batchNum, String errorMessage) {
        log.warn("车辆[{}]车云通信根预置失败: {}", vin, errorMessage);

        // 更新状态为FAILED
        try {
            securityConstant.setPresetState(SecurityConstantState.FAILED);
            securityConstant.setFailReason(truncateDescription(errorMessage));
            securityConstant.setLastAttemptTime(LocalDateTime.now());
            vehSecurityConstantRepository.update(securityConstant);
        } catch (Exception e) {
            log.error("更新车云通信根失败状态异常", e);
        }

        // 写回veh_import_data.description
        try {
            VehImportData vehImportData = vehImportDataRepository.selectByBatchNum(batchNum);
            if (vehImportData != null) {
                String description = vehImportData.getDescription();
                String newDescription = "车云通信根预置失败: " + errorMessage;
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
