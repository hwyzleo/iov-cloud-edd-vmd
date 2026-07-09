package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;

import java.time.LocalDateTime;

/**
 * 安全灌注机注册领域实体
 *
 * @author hwyz_leo
 * @since 2026-07-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvFacilityDevice extends BaseDo<Long> {

    private Long id;
    private String facilityUid;
    private String facilityType;
    private SecurityConstantState presetState;
    private String kmsProvider;
    private String kmsKeyRef;
    private String keySpec;
    private String algorithm;
    private String kcv;
    private String failReason;
    private LocalDateTime genTime;
    private LocalDateTime lastAttemptTime;
    private LocalDateTime createTime;

    /**
     * 初始化实体状态
     */
    public void init() {
        stateInit();
    }
}
