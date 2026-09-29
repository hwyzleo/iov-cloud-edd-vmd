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
     * 调用方声明 ecu_uid（CR-054：OAPI 可传、非权威；MPT 补偿可省略，由 VMD 从绑定解析）
     */
    private String declaredEcuUid;

    /**
     * 证书Profile
     */
    private String certificateProfile;

    /**
     * CSR DER Base64编码
     */
    private String csrDerBase64;

    /**
     * 来源系统（MES / MPT_COMPENSATION）
     */
    private String sourceSystem;

    /**
     * MES原请求号（人工补偿关联，MPT_COMPENSATION 来源可空）
     */
    private String originalRequestId;

    /**
     * 人工补偿原因（MPT 写操作必填，不保存 CSR 全文）
     */
    private String compensationReason;

    /**
     * 关联工单号
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
     * 工厂编号
     */
    private String facilityNo;

    /**
     * 产线代码
     */
    private String lineCode;

}
