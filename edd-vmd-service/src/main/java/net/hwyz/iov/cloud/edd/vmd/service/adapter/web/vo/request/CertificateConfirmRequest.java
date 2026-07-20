package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 证书安装确认请求
 *
 * @author hwyz_leo
 */
@Data
public class CertificateConfirmRequest {

    /**
     * 业务请求ID
     */
    @NotBlank(message = "requestId不能为空")
    private String requestId;

    /**
     * 安装结果：SUCCESS/FAILED
     */
    @NotBlank(message = "result不能为空")
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
