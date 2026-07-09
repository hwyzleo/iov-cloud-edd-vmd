package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 安全灌注机未注册或未就绪异常
 * <p>
 * 防盗根下发时安全灌注机未注册或其设备根未预置
 * （prov_facility_device 无 PRESET 记录）
 *
 * @author hwyz_leo
 * @since 2026-07-08
 */
@Slf4j
public class ProvFacilityNotRegisteredException extends VmdBaseException {

    public ProvFacilityNotRegisteredException(String facilityUid) {
        super(VmdErrorCode.PROV_FACILITY_NOT_REGISTERED);
        log.warn("安全灌注机[{}]未注册或未就绪", facilityUid);
    }
}
