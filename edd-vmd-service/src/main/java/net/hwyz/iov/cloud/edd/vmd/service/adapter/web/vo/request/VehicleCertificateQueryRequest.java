package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

/**
 * 证书申请记录查询请求（MPT，CR-053）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VehicleCertificateQueryRequest extends BaseRequest {

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

}
