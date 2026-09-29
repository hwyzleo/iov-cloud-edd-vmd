package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 证书人工补偿申请命令（MPT，CR-053）
 * <p>
 * 后台不生成设备密钥或 CSR；CSR 必须来自目标 TBOX/安全芯片对应的受信工位回读结果。
 * requestId 已存在时比较请求摘要；originalMesRequestId 命中既有记录时引导对账。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensateCertificateCmd {

    /**
     * 原 MES 请求号或人工补申请号（可选，已存在时进入幂等比对）
     */
    private String requestId;

    /**
     * MES原请求号（可选，关联既有记录时引导对账）
     */
    private String originalMesRequestId;

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
     * CSR DER Base64编码（必填，来自受信工位回读）
     */
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
    private String reason;

    /**
     * 操作人ID
     */
    private String operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 来源IP
     */
    private String sourceIp;

    /**
     * 终端 User-Agent
     */
    private String userAgent;

}
