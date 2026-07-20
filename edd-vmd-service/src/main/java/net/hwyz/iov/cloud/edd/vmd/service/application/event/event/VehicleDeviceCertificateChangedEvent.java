package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.CertificateStatus;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 车辆设备证书变更事件
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public class VehicleDeviceCertificateChangedEvent {

    /**
     * 车辆VIN
     */
    private final String vin;

    /**
     * 绑定ID
     */
    private final Long bindingId;

    /**
     * 零件实例ID
     */
    private final Long partId;

    /**
     * 设备类别
     */
    private final String deviceCategory;

    /**
     * 设备SN
     */
    private final String deviceSn;

    /**
     * 证书序列号
     */
    private final String certSn;

    /**
     * 证书Profile
     */
    private final String certificateProfile;

    /**
     * 证书状态
     */
    private final CertificateStatus certStatus;

    /**
     * 有效期结束时间
     */
    private final LocalDateTime notAfter;

    /**
     * 事件发生时间
     */
    private final Instant occurredAt;

}
