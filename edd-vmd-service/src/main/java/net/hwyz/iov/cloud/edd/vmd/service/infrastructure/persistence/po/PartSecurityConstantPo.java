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
 * 零件安全常量表 持久化对象
 *
 * @author hwyz_leo
 * @since 2026-06-24
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_part_security_constant")
public class PartSecurityConstantPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 零件编码
     */
    @TableField("part_code")
    private String partCode;

    /**
     * 零件序列号
     */
    @TableField("sn")
    private String sn;

    /**
     * 安全芯片/HSM唯一标识快照
     */
    @TableField("chip_uid")
    private String chipUid;

    /**
     * 安全常量用途/类型
     */
    @TableField("constant_type")
    private String constantType;

    /**
     * KMS/HSM提供方标识
     */
    @TableField("kms_provider")
    private String kmsProvider;

    /**
     * KMS密钥引用
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
     * 预置状态
     */
    @TableField("preset_state")
    private String presetState;

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
     * 失败原因
     */
    @TableField("fail_reason")
    private String failReason;

    /**
     * 来源导入批次号
     */
    @TableField("batch_num")
    private String batchNum;

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
