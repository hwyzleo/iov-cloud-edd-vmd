package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 车辆设备证书表 持久化对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-20
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_certificate")
public class VehicleCertificatePo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * MES/OAPI业务请求幂等键
     */
    @TableField("request_id")
    private String requestId;

    /**
     * framework/PKI返回的申请编号
     */
    @TableField("pki_request_id")
    private String pkiRequestId;

    /**
     * PKI证书序列号
     */
    @TableField("cert_sn")
    private String certSn;

    /**
     * 签发时车辆VIN快照
     */
    @TableField("vin")
    private String vin;

    /**
     * 签发时active vehicle_part.id
     */
    @TableField("binding_id")
    private Long bindingId;

    /**
     * 设备物理实例，关联part_info.id
     */
    @TableField("part_id")
    private Long partId;

    /**
     * 设备类别
     */
    @TableField("device_category")
    private String deviceCategory;

    /**
     * 证书CN对应的稳定设备身份/芯片UID
     */
    @TableField("device_sn")
    private String deviceSn;

    /**
     * 受治理证书模板
     */
    @TableField("certificate_profile")
    private String certificateProfile;

    /**
     * CSR SHA-256指纹
     */
    @TableField("csr_fingerprint")
    private String csrFingerprint;

    /**
     * X.509 Subject
     */
    @TableField("subject")
    private String subject;

    /**
     * X.509 Issuer
     */
    @TableField("issuer")
    private String issuer;

    /**
     * 证书SHA-256指纹
     */
    @TableField("certificate_fingerprint")
    private String certificateFingerprint;

    /**
     * 证书有效期开始时间
     */
    @TableField("not_before")
    private LocalDateTime notBefore;

    /**
     * 证书有效期结束时间
     */
    @TableField("not_after")
    private LocalDateTime notAfter;

    /**
     * 证书状态
     */
    @TableField("cert_status")
    private String certStatus;

    /**
     * 签发时间
     */
    @TableField("issued_at")
    private LocalDateTime issuedAt;

    /**
     * 安装确认时间
     */
    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;

    /**
     * 最近失败原因
     */
    @TableField("fail_reason")
    private String failReason;

    /**
     * 来源系统
     */
    @TableField("source_system")
    private String sourceSystem;

    /**
     * 工厂编号
     */
    @TableField("facility_no")
    private String facilityNo;

    /**
     * 产线代码
     */
    @TableField("line_code")
    private String lineCode;
}
