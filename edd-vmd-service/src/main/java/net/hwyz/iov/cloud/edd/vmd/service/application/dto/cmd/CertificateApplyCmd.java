package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 证书申请命令
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateApplyCmd {

    /**
     * MES/OAPI业务请求幂等键
     */
    private String requestId;

    /**
     * 车辆VIN
     */
    private String vin;

    /**
     * 设备类别
     */
    private String deviceCategory;

    /**
     * 设备SN
     */
    private String deviceSn;

    /**
     * 证书Profile
     */
    private String certificateProfile;

    /**
     * CSR DER Base64编码
     */
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
