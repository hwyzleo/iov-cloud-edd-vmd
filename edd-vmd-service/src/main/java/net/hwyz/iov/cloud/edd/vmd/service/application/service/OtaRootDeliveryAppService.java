package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OtaRootResult;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.OtaRootNotPresetException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ProvFacilityNotRegisteredException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ProvFacilityDevice;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.ProvFacilityDeviceRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehSecurityConstantRepository;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.ProvisioningResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * OTA根下发应用服务
 * <p>
 * 经 framework-security wrapFor 派生并封装到灌注机，明文不出 KMS。
 * wrapped 为一次性下发密文，VMD 不落库，仅审计引用与 KCV。
 *
 * @author hwyz_leo
 * @since 2026-07-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtaRootDeliveryAppService {

    private final VehSecurityConstantRepository vehSecurityConstantRepository;
    private final ProvFacilityDeviceRepository provFacilityDeviceRepository;
    private final KeyProvisioningTemplate keyProvisioningTemplate;

    private static final String CONSTANT_TYPE_OTA = "OTA";

    /**
     * 派生OTA根并封装到指定灌注机
     *
     * @param vin         车架号
     * @param facilityUid 灌注机唯一标识
     * @return 封装结果（kmsKeyRef, kcv, wrapped）
     */
    public OtaRootResult deriveOtaRootForFacility(String vin, String facilityUid) {
        log.info("开始下发OTA根, vin={}, facilityUid={}", vin, facilityUid);

        VehSecurityConstant otaRoot = vehSecurityConstantRepository.selectByVinAndConstantType(vin, CONSTANT_TYPE_OTA);
        if (otaRoot == null || otaRoot.getPresetState() != SecurityConstantState.PRESET) {
            throw new OtaRootNotPresetException(vin);
        }

        ProvFacilityDevice facility = provFacilityDeviceRepository.selectByFacilityUid(facilityUid);
        if (facility == null || facility.getPresetState() != SecurityConstantState.PRESET) {
            throw new ProvFacilityNotRegisteredException(facilityUid);
        }

        ProvisioningResult result = keyProvisioningTemplate.wrapFor(BizType.OTA_VEHICLE_ROOT, vin, facilityUid);

        log.info("OTA根下发成功, vin={}, facilityUid={}, kmsKeyRef={}, 下发时间={}",
                vin, facilityUid, result.getKmsKeyRef(), LocalDateTime.now());

        return OtaRootResult.builder()
                .kmsKeyRef(result.getKmsKeyRef())
                .kcv(bytesToHex(result.getKcv()))
                .wrapped(result.getWrappedMaterial())
                .build();
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
