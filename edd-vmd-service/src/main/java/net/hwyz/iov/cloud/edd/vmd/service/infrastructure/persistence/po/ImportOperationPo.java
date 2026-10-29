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
 * 统一导入操作主表 持久化对象（CR-071）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_import_operation")
public class ImportOperationPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("operation_id")
    private String operationId;

    @TableField("domain")
    private String domain;

    @TableField("operation_type")
    private String operationType;

    @TableField("import_data_id")
    private Long importDataId;

    @TableField("batch_num")
    private String batchNum;

    @TableField("original_import_data_id")
    private Long originalImportDataId;

    @TableField("payload_digest")
    private byte[] payloadDigest;

    @TableField("request_digest")
    private byte[] requestDigest;

    @TableField("plan_digest")
    private byte[] planDigest;

    @TableField("plan_json")
    private String planJson;

    @TableField("operator_id")
    private String operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("operation_source")
    private String operationSource;

    @TableField("reason")
    private String reason;

    @TableField("ticket_no")
    private String ticketNo;

    @TableField("status")
    private String status;

    @TableField("counters_json")
    private String countersJson;

    @TableField("legacy_replay_id")
    private String legacyReplayId;

    @TableField("lease_owner")
    private String leaseOwner;

    @TableField("lease_until")
    private LocalDateTime leaseUntil;

    @TableField("fence_token")
    private Long fenceToken;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

}
