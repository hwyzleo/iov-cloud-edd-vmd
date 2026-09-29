package net.hwyz.iov.cloud.edd.vmd.service.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 证书申请记录查询条件（MPT，CR-053）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCertificateQuery {

    /**
     * 业务请求ID
     */
    private String requestId;

    /**
     * 车辆VIN
     */
    private String vin;

    /**
     * 设备SN
     */
    private String deviceSn;

    /**
     * 证书序列号
     */
    private String certSn;

    /**
     * 证书状态
     */
    private String status;

    /**
     * 来源系统（MES / MPT_COMPENSATION）
     */
    private String source;

    /**
     * 起始时间
     */
    private LocalDateTime beginTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

}
