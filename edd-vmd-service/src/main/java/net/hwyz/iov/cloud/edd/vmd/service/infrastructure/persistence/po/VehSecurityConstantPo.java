package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 车辆安全常量表 持久化对象
 * 
 * @author hwyz_leo
 * @since 2026-06-17
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_security_constant")
public class VehSecurityConstantPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 车架号
     */
    @TableField("vin")
    private String vin;

    /**
     * 导入批次号
     */
    @TableField("batch_num")
    private String batchNum;

    /**
     * 预置状态：PENDING-待预置，PRESET-已预置，FAILED-预置失败
     */
    @TableField("preset_state")
    private String presetState;

    /**
     * KMS/HSM 提供方标识
     */
    @TableField("kms_provider")
    private String kmsProvider;

    /**
     * KMS密钥引用（keyId/alias，仅为指针）
     */
    @TableField("kms_key_ref")
    private String kmsKeyRef;

    /**
     * 密钥规格
     */
    @TableField("key_spec")
    private String keySpec;

    /**
     * 算法标识
     */
    @TableField("algorithm")
    private String algorithm;

    /**
     * KCV密钥校验值（可公开、不可逆、非密钥，hex编码）
     */
    @TableField("kcv")
    private String kcv;

    /**
     * 失败原因（按列长截断）
     */
    @TableField("fail_reason")
    private String failReason;

    /**
     * 生成成功时间
     */
    @TableField("gen_time")
    private LocalDateTime genTime;

    /**
     * 最后尝试时间
     */
    @TableField("last_attempt_time")
    private LocalDateTime lastAttemptTime;

    /**
     * 常量类型（预留扩展位）
     */
    @TableField("constant_type")
    private String constantType;

    /**
     * 灌注确认状态（CR-043）
     * true: 已确认
     * false: 未确认
     */
    @TableField("provision_confirmed")
    private Boolean provisionConfirmed;

    /**
     * 灌注确认时间（CR-043）
     */
    @TableField("confirm_time")
    private LocalDateTime confirmTime;

    /**
     * 灌注确认来源（CR-043）
     */
    @TableField("confirm_source")
    private String confirmSource;
}
