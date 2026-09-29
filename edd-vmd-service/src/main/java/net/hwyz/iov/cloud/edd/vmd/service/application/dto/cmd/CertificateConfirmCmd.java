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
     * 证书序列号（共享确认内核校验 requestId + certSn + deviceSn，CR-053）
     */
    private String certSn;

    /**
     * 人工原因（MPT 安装结果补录必填，不保存 CSR 全文）
     */
    private String reason;

    /**
     * 关联工单号（MPT 安装结果补录必填）
     */
    private String ticketNo;

    /**
     * 操作人ID（MPT 人工操作审计）
     */
    private String operatorId;

    /**
     * 操作人姓名（MPT 人工操作审计）
     */
    private String operatorName;

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
