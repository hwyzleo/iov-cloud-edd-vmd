package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.CertificateStatus;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 车辆设备证书领域实体
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class VehicleCertificate implements DomainObj<VehicleCertificate> {

    /**
     * 主键
     */
    private Long id;

    /**
     * MES/OAPI业务请求幂等键
     */
    private String requestId;

    /**
     * framework/PKI返回的申请编号
     */
    private String pkiRequestId;

    /**
     * PKI证书序列号
     */
    private String certSn;

    /**
     * 签发时车辆VIN快照
     */
    private String vin;

    /**
     * 签发时active vehicle_part.id
     */
    private Long bindingId;

    /**
     * 设备物理实例，关联part_info.id
     */
    private Long partId;

    /**
     * 设备类别
     */
    private String deviceCategory;

    /**
     * 证书CN对应的稳定设备身份/芯片UID
     */
    private String deviceSn;

    /**
     * 受治理证书模板
     */
    private String certificateProfile;

    /**
     * CSR SHA-256指纹
     */
    private String csrFingerprint;

    /**
     * X.509 Subject
     */
    private String subject;

    /**
     * X.509 Issuer
     */
    private String issuer;

    /**
     * 证书SHA-256指纹
     */
    private String certificateFingerprint;

    /**
     * 证书有效期开始时间
     */
    private LocalDateTime notBefore;

    /**
     * 证书有效期结束时间
     */
    private LocalDateTime notAfter;

    /**
     * 证书状态
     */
    private CertificateStatus certStatus;

    /**
     * 签发时间
     */
    private LocalDateTime issuedAt;

    /**
     * 安装确认时间
     */
    private LocalDateTime confirmedAt;

    /**
     * 最近失败原因
     */
    private String failReason;

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

    /**
     * 创建时间
     */
    private Instant createTime;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 修改时间
     */
    private Instant modifyTime;

    /**
     * 修改者
     */
    private String modifyBy;

    /**
     * 记录版本
     */
    private Integer rowVersion;

    /**
     * 记录是否有效
     */
    private Boolean rowValid;

}
