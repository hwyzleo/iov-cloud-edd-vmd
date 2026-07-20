package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 证书申请请求
 *
 * @author hwyz_leo
 */
@Data
public class CertificateApplyRequest {

    /**
     * MES/OAPI业务请求幂等键
     */
    @NotBlank(message = "requestId不能为空")
    private String requestId;

    /**
     * 车辆VIN
     */
    @NotBlank(message = "vin不能为空")
    private String vin;

    /**
     * 设备类别
     */
    @NotBlank(message = "deviceCategory不能为空")
    private String deviceCategory;

    /**
     * 设备SN
     */
    @NotBlank(message = "deviceSn不能为空")
    private String deviceSn;

    /**
     * 证书Profile
     */
    @NotBlank(message = "certificateProfile不能为空")
    private String certificateProfile;

    /**
     * CSR DER Base64编码
     */
    @NotBlank(message = "csrDerBase64不能为空")
    private String csrDerBase64;

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
