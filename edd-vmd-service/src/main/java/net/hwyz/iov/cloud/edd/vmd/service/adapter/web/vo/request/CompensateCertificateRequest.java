package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

/**
 * 证书人工补偿申请请求（MPT，CR-053）
 * <p>
 * 后台不生成设备密钥或 CSR；CSR 必须来自目标 TBOX/安全芯片对应的受信工位回读结果。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CompensateCertificateRequest extends BaseRequest {

    /**
     * 原 MES 请求号或人工补申请号（可选，已存在时进入幂等比对）
     */
    private String requestId;

    /**
     * MES原请求号（可选，命中既有记录时引导对账）
     */
    private String originalMesRequestId;

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
     * CSR DER Base64编码（必填，来自受信工位回读）
     */
    @NotBlank(message = "csrDerBase64不能为空")
    private String csrDerBase64;

    /**
     * 工厂编号
     */
    private String facilityNo;

    /**
     * 产线代码
     */
    private String lineCode;

    /**
     * 工单号
     */
    private String ticketNo;

    /**
     * 人工补偿原因（必填）
     */
    @NotBlank(message = "reason不能为空")
    private String reason;

}
