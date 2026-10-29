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
 * 统一导入操作明细表 持久化对象（CR-071）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_import_operation_item")
public class ImportOperationItemPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("operation_id")
    private String operationId;

    @TableField("item_key")
    private byte[] itemKey;

    @TableField("candidate_key")
    private String candidateKey;

    @TableField("entity_type")
    private String entityType;

    @TableField("entity_id")
    private Long entityId;

    @TableField("vin")
    private String vin;

    @TableField("action_type")
    private String actionType;

    @TableField("phase")
    private String phase;

    @TableField("status")
    private String status;

    @TableField("source")
    private String source;

    @TableField("source_event_id")
    private String sourceEventId;

    @TableField("content_digest")
    private byte[] contentDigest;

    @TableField("expected_row_version")
    private Long expectedRowVersion;

    @TableField("result_row_version")
    private Long resultRowVersion;

    @TableField("changed_fields")
    private String changedFields;

    @TableField("event_id")
    private String eventId;

    @TableField("outbox_id")
    private Long outboxId;

    @TableField("legacy_item_ref")
    private String legacyItemRef;

    @TableField("attempt_count")
    private Integer attemptCount;

    @TableField("skip_reason")
    private String skipReason;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_message")
    private String errorMessage;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

}
