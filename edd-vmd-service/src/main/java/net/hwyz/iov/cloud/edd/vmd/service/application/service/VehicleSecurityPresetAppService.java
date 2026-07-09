package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehSecurityConstantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.ProvisioningResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 车辆安全常量预置应用服务
 * <p>
 * 在 PRODUCE 建档同一编排内派生车云通信根（ROOT→V2C_COMM_ROOT）
 * 与防盗根（IMMO→IMMO_GROUP_KEY），密钥明文不出 KMS。
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

    private static final int DESCRIPTION_MAX_LENGTH = 500;

    private static final String CONSTANT_TYPE_ROOT = "ROOT";
    private static final String CONSTANT_TYPE_IMMO = "IMMO";

    private static final Map<String, BizType> CONSTANT_TYPE_BIZ_TYPE_MAP = new LinkedHashMap<>();

    static {
        CONSTANT_TYPE_BIZ_TYPE_MAP.put(CONSTANT_TYPE_ROOT, BizType.V2C_COMM_ROOT);
        CONSTANT_TYPE_BIZ_TYPE_MAP.put(CONSTANT_TYPE_IMMO, BizType.IMMO_GROUP_KEY);
    }

    /**
     * 预置车云通信根与防盗根
     * <p>
     * 在 PRODUCE 建档成功后同步触发，遍历 ROOT / IMMO 两种常量类型，
     * 各自独立派生、幂等、失败不阻断另一类型、不回滚建档。
     *
     * @param vin      车架号
     * @param batchNum 批次号
     */
    @Transactional(rollbackFor = Exception.class)
    public void preset(String vin, String batchNum) {
        log.info("开始预置车辆[{}]安全常量, batchNum={}", vin, batchNum);

        if (batchNum != null && batchNum.startsWith("EOL-")) {
            log.debug("EOL 补发的 PRODUCE 事件，不触发安全预置: {}", vin);
            return;
        }

        for (Map.Entry<String, BizType> entry : CONSTANT_TYPE_BIZ_TYPE_MAP.entrySet()) {
            presetSingleType(vin, batchNum, entry.getKey(), entry.getValue());
        }
    }

    /**
     * 预置单个常量类型
     */
    private void presetSingleType(String vin, String batchNum, String constantType, BizType bizType) {
        String typeLabel = CONSTANT_TYPE_ROOT.equals(constantType) ? "车云通信根" : "防盗根";

        VehSecurityConstant existing = vehSecurityConstantRepository.selectByVinAndConstantType(vin, constantType);

        if (existing != null && existing.getPresetState() == SecurityConstantState.PRESET) {
            log.info("车辆[{}]{}已预置，跳过", vin, typeLabel);
            return;
        }

        VehSecurityConstant securityConstant;
        if (existing == null) {
            securityConstant = VehSecurityConstant.builder()
                    .vin(vin)
                    .batchNum(batchNum)
                    .presetState(SecurityConstantState.PENDING)
                    .constantType(constantType)
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
            ProvisioningResult result = keyProvisioningTemplate.deriveByVin(vin, bizType);

            securityConstant.setPresetState(SecurityConstantState.PRESET);
            securityConstant.setKmsKeyRef(result.getKmsKeyRef());
            securityConstant.setKeySpec(result.getKeySpec());
            securityConstant.setKmsProvider(result.getProvider());
            securityConstant.setAlgorithm(result.getAlgorithm());
            securityConstant.setKcv(bytesToHex(result.getKcv()));
            securityConstant.setGenTime(LocalDateTime.now());
            securityConstant.setLastAttemptTime(LocalDateTime.now());
            vehSecurityConstantRepository.update(securityConstant);

            log.info("车辆[{}]{}预置成功", vin, typeLabel);
        } catch (CryptoDependencyUnavailableException e) {
            handlePresetFailure(securityConstant, vin, batchNum, constantType, typeLabel,
                    "KMS/HSM服务不可用: " + e.getMessage());
        } catch (Exception e) {
            handlePresetFailure(securityConstant, vin, batchNum, constantType, typeLabel, e.getMessage());
        }
    }

    /**
     * 处理预置失败
     */
    private void handlePresetFailure(VehSecurityConstant securityConstant, String vin, String batchNum,
                                     String constantType, String typeLabel, String errorMessage) {
        log.warn("车辆[{}]{}预置失败: {}", vin, typeLabel, errorMessage);

        try {
            securityConstant.setPresetState(SecurityConstantState.FAILED);
            securityConstant.setFailReason(truncateDescription(errorMessage));
            securityConstant.setLastAttemptTime(LocalDateTime.now());
            vehSecurityConstantRepository.update(securityConstant);
        } catch (Exception e) {
            log.error("更新{}失败状态异常", typeLabel, e);
        }

        try {
            VehImportData vehImportData = vehImportDataRepository.selectByBatchNum(batchNum);
            if (vehImportData != null) {
                String description = vehImportData.getDescription();
                String newDescription = typeLabel + "预置失败: " + errorMessage;
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
