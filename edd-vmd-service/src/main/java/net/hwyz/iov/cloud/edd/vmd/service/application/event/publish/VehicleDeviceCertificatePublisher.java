package net.hwyz.iov.cloud.edd.vmd.service.application.event.publish;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleDeviceCertificateChangedEvent;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificate;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 车辆设备证书变更事件发布类
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleDeviceCertificatePublisher {

    private final ApplicationContext ctx;

    /**
     * 发布证书变更事件
     *
     * @param certificate 证书信息
     */
    public void publishCertificateChanged(VehicleCertificate certificate) {
        log.info("发布车辆[{}]设备证书变更事件，证书SN[{}]，状态[{}]",
                certificate.getVin(), certificate.getCertSn(), certificate.getCertStatus());

        VehicleDeviceCertificateChangedEvent event = new VehicleDeviceCertificateChangedEvent(
                certificate.getVin(),
                certificate.getBindingId(),
                certificate.getPartId(),
                certificate.getDeviceCategory(),
                certificate.getDeviceSn(),
                certificate.getCertSn(),
                certificate.getCertificateProfile(),
                certificate.getCertStatus(),
                certificate.getNotAfter(),
                Instant.now()
        );

        ctx.publishEvent(event);
    }

}
