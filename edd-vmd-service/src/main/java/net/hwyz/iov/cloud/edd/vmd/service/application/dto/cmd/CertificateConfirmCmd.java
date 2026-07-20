package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 证书安装确认命令
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateConfirmCmd {

    /**
     * 业务请求ID
     */
    private String requestId;

    /**
     * 安装结果：SUCCESS/FAILED
     */
    private String result;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 车辆VIN
     */
    private String vin;

    /**
     * 设备SN
     */
    private String deviceSn;

    /**
     * 来源系统
     */
    private String sourceSystem;

    /**
     * 工厂编号
     */
    private String facilityNo;

    /**
     * 产线代码
     */
    private String lineCode;

}
