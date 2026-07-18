package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EOL放行门禁服务类
 * <p>
 * 处理EOL_RESULT放行门禁逻辑：
 * 1. 解析EOL_RESULT状态（PASS/NG/REWORK）
 * 2. 写入veh_basic_info.eol_result字段
 * 3. NG/REWORK状态抑制后续放行，但不阻断落库
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EolResultGateService {

    private final VehBasicInfoRepository vehBasicInfoRepository;

    /**
     * 处理EOL_RESULT放行门禁
     *
     * @param vin 车架号
     * @param eolResult EOL结果状态（PASS/NG/REWORK）
     * @return 是否允许后续放行
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean processEolResult(String vin, String eolResult) {
        log.debug("处理EOL_RESULT放行门禁: vin={}, eolResult={}", vin, eolResult);
        
        // 1. 查询车辆基础信息
        VehicleBasicInfo vehicleBasicInfo = vehBasicInfoRepository.selectByVin(vin);
        if (vehicleBasicInfo == null) {
            log.warn("车辆[{}]不存在，无法处理EOL_RESULT", vin);
            return false;
        }
        
        // 2. 更新eol_result字段
        vehicleBasicInfo.setEolResult(eolResult);
        vehBasicInfoRepository.update(vehicleBasicInfo);
        
        // 3. 判断是否允许后续放行
        boolean allowRelease = "PASS".equalsIgnoreCase(eolResult);
        
        if (!allowRelease) {
            log.info("车辆[{}]EOL_RESULT为[{}]，抑制后续放行", vin, eolResult);
        }
        
        return allowRelease;
    }

    /**
     * 检查车辆是否允许放行
     *
     * @param vin 车架号
     * @return 是否允许放行
     */
    public boolean isVehicleAllowedToRelease(String vin) {
        VehicleBasicInfo vehicleBasicInfo = vehBasicInfoRepository.selectByVin(vin);
        if (vehicleBasicInfo == null) {
            return false;
        }
        
        // 如果eol_result为空或PASS，允许放行
        String eolResult = vehicleBasicInfo.getEolResult();
        return eolResult == null || "PASS".equalsIgnoreCase(eolResult);
    }
}
