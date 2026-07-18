package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehSecurityConstantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSecurityConstantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 安全回执对账服务类
 * <p>
 * 消费EOL电检回执，将「下发即记账」升级为「产线灌注确认对账」
 * 处理逻辑：
 * 1. 检查本地preset_state是否为PRESET
 * 2. 如果是，则置provision_confirmed=true
 * 3. 不一致时写告警并保留差异待人工核
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityProvisionConfirmService {

    private final VehSecurityConstantRepository vehSecurityConstantRepository;
    private final PartSecurityConstantRepository partSecurityConstantRepository;

    /**
     * 处理车辆级安全常量灌注确认
     *
     * @param vin 车架号
     * @param constantType 常量类型（ROOT/IMMO/OTA）
     * @param provisionStatus 灌注状态（PROVISIONED/NOT_PROVISIONED）
     * @param confirmSource 确认来源（EOL/MANUAL）
     */
    @Transactional(rollbackFor = Exception.class)
    public void processVehicleSecurityProvision(String vin, String constantType, String provisionStatus, String confirmSource) {
        log.debug("处理车辆级安全常量灌注确认: vin={}, constantType={}, status={}", vin, constantType, provisionStatus);
        
        // 1. 查询本地安全常量记录
        VehSecurityConstant vehSecurityConstant = vehSecurityConstantRepository.selectByVinAndConstantType(vin, constantType);
        
        if (vehSecurityConstant == null) {
            log.warn("车辆[{}]安全常量类型[{}]不存在，无法处理灌注确认", vin, constantType);
            return;
        }
        
        // 2. 检查本地preset_state是否为PRESET
        if (!"PRESET".equals(vehSecurityConstant.getPresetState())) {
            log.warn("车辆[{}]安全常量类型[{}]状态为[{}]，非PRESET状态，无法确认灌注", 
                    vin, constantType, vehSecurityConstant.getPresetState());
            return;
        }
        
        // 3. 处理灌注状态
        if ("PROVISIONED".equalsIgnoreCase(provisionStatus)) {
            // 更新为已确认
            vehSecurityConstant.setProvisionConfirmed(true);
            vehSecurityConstant.setConfirmTime(LocalDateTime.now());
            vehSecurityConstant.setConfirmSource(confirmSource);
            vehSecurityConstantRepository.update(vehSecurityConstant);
            
            log.info("车辆[{}]安全常量类型[{}]灌注确认成功", vin, constantType);
        } else {
            log.warn("车辆[{}]安全常量类型[{}]灌注状态为[{}]，不一致，保留差异待人工核", 
                    vin, constantType, provisionStatus);
        }
    }

    /**
     * 处理器件级安全常量灌注确认
     *
     * @param partCode 零件编码
     * @param sn 零件序列号
     * @param constantType 常量类型
     * @param provisionStatus 灌注状态
     * @param confirmSource 确认来源
     */
    @Transactional(rollbackFor = Exception.class)
    public void processPartSecurityProvision(String partCode, String sn, String constantType, String provisionStatus, String confirmSource) {
        log.debug("处理器件级安全常量灌注确认: partCode={}, sn={}, constantType={}, status={}", 
                partCode, sn, constantType, provisionStatus);
        
        // 1. 查询本地安全常量记录
        PartSecurityConstant partSecurityConstant = partSecurityConstantRepository.selectByPartCodeAndSn(partCode, sn);
        
        if (partSecurityConstant == null) {
            log.warn("零件[{}-{}]安全常量类型[{}]不存在，无法处理灌注确认", partCode, sn, constantType);
            return;
        }
        
        // 2. 检查本地preset_state是否为PRESET
        if (!"PRESET".equals(partSecurityConstant.getPresetState())) {
            log.warn("零件[{}-{}]安全常量类型[{}]状态为[{}]，非PRESET状态，无法确认灌注", 
                    partCode, sn, constantType, partSecurityConstant.getPresetState());
            return;
        }
        
        // 3. 处理灌注状态
        if ("PROVISIONED".equalsIgnoreCase(provisionStatus)) {
            // 更新为已确认
            partSecurityConstant.setProvisionConfirmed(true);
            partSecurityConstant.setConfirmTime(LocalDateTime.now());
            partSecurityConstant.setConfirmSource(confirmSource);
            partSecurityConstantRepository.update(partSecurityConstant);
            
            log.info("零件[{}-{}]安全常量类型[{}]灌注确认成功", partCode, sn, constantType);
        } else {
            log.warn("零件[{}-{}]安全常量类型[{}]灌注状态为[{}]，不一致，保留差异待人工核", 
                    partCode, sn, constantType, provisionStatus);
        }
    }
}
