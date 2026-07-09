package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ProvFacilityDevice;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.ProvFacilityDeviceRepository;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CryptoDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.ProvisioningResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 安全灌注机注册与设备根预置应用服务
 *
 * @author hwyz_leo
 * @since 2026-07-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProvFacilityPresetAppService {

    private final ProvFacilityDeviceRepository provFacilityDeviceRepository;
    private final KeyProvisioningTemplate keyProvisioningTemplate;

    private static final String FACILITY_TYPE_KLD = "KLD";

    /**
     * 注册安全灌注机并预置设备根
     * <p>
     * 经 deriveByUid(facilityUid, KLD_DEVICE_ROOT) 预置设备根，按 facility_uid 幂等。
     *
     * @param facilityUid 灌注机唯一标识
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(String facilityUid) {
        log.info("开始注册安全灌注机[{}]", facilityUid);

        ProvFacilityDevice existing = provFacilityDeviceRepository.selectByFacilityUid(facilityUid);

        if (existing != null && existing.getPresetState() == SecurityConstantState.PRESET) {
            log.info("安全灌注机[{}]已注册且设备根已预置，跳过", facilityUid);
            return;
        }

        ProvFacilityDevice device;
        if (existing == null) {
            device = ProvFacilityDevice.builder()
                    .facilityUid(facilityUid)
                    .facilityType(FACILITY_TYPE_KLD)
                    .presetState(SecurityConstantState.PENDING)
                    .createTime(LocalDateTime.now())
                    .build();
            device.init();
            provFacilityDeviceRepository.insert(device);
        } else {
            device = existing;
            device.setPresetState(SecurityConstantState.PENDING);
        }

        try {
            ProvisioningResult result = keyProvisioningTemplate.deriveByUid(facilityUid, BizType.KLD_DEVICE_ROOT);

            device.setPresetState(SecurityConstantState.PRESET);
            device.setKmsKeyRef(result.getKmsKeyRef());
            device.setKeySpec(result.getKeySpec());
            device.setKmsProvider(result.getProvider());
            device.setAlgorithm(result.getAlgorithm());
            device.setKcv(bytesToHex(result.getKcv()));
            device.setGenTime(LocalDateTime.now());
            device.setLastAttemptTime(LocalDateTime.now());
            provFacilityDeviceRepository.update(device);

            log.info("安全灌注机[{}]设备根预置成功", facilityUid);
        } catch (CryptoDependencyUnavailableException e) {
            handleRegisterFailure(device, facilityUid, "KMS/HSM服务不可用: " + e.getMessage());
        } catch (Exception e) {
            handleRegisterFailure(device, facilityUid, e.getMessage());
        }
    }

    private void handleRegisterFailure(ProvFacilityDevice device, String facilityUid, String errorMessage) {
        log.warn("安全灌注机[{}]设备根预置失败: {}", facilityUid, errorMessage);
        try {
            device.setPresetState(SecurityConstantState.FAILED);
            device.setFailReason(truncateDescription(errorMessage));
            device.setLastAttemptTime(LocalDateTime.now());
            provFacilityDeviceRepository.update(device);
        } catch (Exception e) {
            log.error("更新灌注机失败状态异常", e);
        }
    }

    private String truncateDescription(String description) {
        if (description == null) {
            return null;
        }
        if (description.length() <= 500) {
            return description;
        }
        return description.substring(0, 497) + "...";
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
