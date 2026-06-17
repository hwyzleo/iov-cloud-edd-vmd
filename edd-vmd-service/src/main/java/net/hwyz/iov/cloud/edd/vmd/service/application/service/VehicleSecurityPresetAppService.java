package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.KmsHsmUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.SecurityConstantPresetFailedException;
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
                    .build();
            securityConstant.init();
            vehSecurityConstantRepository.insert(securityConstant);
        } else {
            securityConstant = existing;
            securityConstant.setPresetState(SecurityConstantState.PENDING);
            securityConstant.setBatchNum(batchNum);
        }
        
        try {
            // 调用KMS/HSM生成安全常量
            // TODO: 集成KMS/HSM客户端
            // KmsHsmResult result = kmsHsmClient.generatePerVinConstant(vin);
            
            // 模拟成功
            String keyHandle = "mock_key_handle_" + vin;
            String cipherBlob = "mock_cipher_blob_" + vin;
            
            // 更新为预置成功
            securityConstant.setPresetState(SecurityConstantState.PRESET);
            securityConstant.setKeyHandle(keyHandle);
            securityConstant.setCipherBlob(cipherBlob);
            securityConstant.setGenTime(LocalDateTime.now());
            securityConstant.setLastAttemptTime(LocalDateTime.now());
            vehSecurityConstantRepository.update(securityConstant);
            
            log.info("车辆[{}]安全常量预置成功", vin);
        } catch (KmsHsmUnavailableException e) {
            // KMS/HSM不可用，记录失败
            handlePresetFailure(securityConstant, vin, batchNum, e.getMessage());
        } catch (SecurityConstantPresetFailedException e) {
            // 预置失败，记录失败
            handlePresetFailure(securityConstant, vin, batchNum, e.getMessage());
        } catch (Exception e) {
            // 其他异常，记录失败
            handlePresetFailure(securityConstant, vin, batchNum, e.getMessage());
        }
    }
    
    /**
     * 处理预置失败
     */
    private void handlePresetFailure(VehSecurityConstant securityConstant, String vin, String batchNum, String errorMessage) {
        log.warn("车辆[{}]安全常量预置失败: {}", vin, errorMessage);
        
        // 更新状态为FAILED
        securityConstant.setPresetState(SecurityConstantState.FAILED);
        securityConstant.setFailReason(errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage);
        securityConstant.setLastAttemptTime(LocalDateTime.now());
        vehSecurityConstantRepository.update(securityConstant);
        
        // 写回veh_import_data.description
        try {
            net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData vehImportData = 
                    vehImportDataRepository.selectByBatchNum(batchNum);
            if (vehImportData != null) {
                String description = vehImportData.getDescription();
                String newDescription = "安全常量预置失败: " + errorMessage;
                if (description != null) {
                    newDescription = description + "; " + newDescription;
                }
                // 截断处理
                if (newDescription.length() > 500) {
                    newDescription = newDescription.substring(0, 500);
                }
                vehImportData.setDescription(newDescription);
                vehImportDataRepository.update(vehImportData);
            }
        } catch (Exception e) {
            log.error("写回veh_import_data.description失败", e);
        }
    }
}
