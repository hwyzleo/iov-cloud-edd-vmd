package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

import java.time.LocalDateTime;

/**
 * 零件实例属性变更历史表 持久化对象（CR-071）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_part_info_change_log")
public class PartInfoChangeLogPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("part_id")
    private Long partId;

    @TableField("change_type")
    private String changeType;

    @TableField("before_json")
    private String beforeJson;

    @TableField("after_json")
    private String afterJson;

    @TableField("changed_fields")
    private String changedFields;

    @TableField("source")
    private String source;

    @TableField("operation_source")
    private String operationSource;

    @TableField("operator_id")
    private String operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("change_time")
    private LocalDateTime changeTime;

    @TableField("source_event_time")
    private LocalDateTime sourceEventTime;

    @TableField("source_event_id")
    private String sourceEventId;

    @TableField("inbound_batch_no")
    private String inboundBatchNo;

    @TableField("operation_id")
    private String operationId;

    @TableField("mutation_key")
    private byte[] mutationKey;

    /**
     * 业务本体提交版本（tb_part_info.row_version），复用 BasePo.rowVersion 列
     */
    @TableField("metadata_json")
    private String metadataJson;

}
