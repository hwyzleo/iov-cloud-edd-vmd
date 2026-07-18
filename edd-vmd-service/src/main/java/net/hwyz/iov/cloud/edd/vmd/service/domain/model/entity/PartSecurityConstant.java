package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;

import java.time.LocalDateTime;

/**
 * 零件安全常量领域实体
 *
 * @author hwyz_leo
 * @since 2026-06-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartSecurityConstant extends BaseDo<Long> {

    private Long id;
    private String partCode;
    private String sn;
    private String chipUid;
    private String constantType;
    private String kmsProvider;
    private String kmsKeyRef;
    private String keySpec;
    private String algorithm;
    private String kcv;
    private SecurityConstantState presetState;
    private LocalDateTime genTime;
    private LocalDateTime lastAttemptTime;
    private String failReason;
    private String batchNum;
    private Boolean provisionConfirmed;
    private LocalDateTime confirmTime;
    private String confirmSource;
    private LocalDateTime createTime;

    /**
     * 初始化实体状态
     */
    public void init() {
        stateInit();
    }
}
