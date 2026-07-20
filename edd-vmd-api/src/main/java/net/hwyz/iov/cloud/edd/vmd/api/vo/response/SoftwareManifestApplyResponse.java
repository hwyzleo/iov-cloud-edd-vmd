package net.hwyz.iov.cloud.edd.vmd.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 软件实装清单写入结果
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoftwareManifestApplyResponse {

    /**
     * 成功写入的条目数
     */
    private Integer applied;

    /**
     * 被版本时序gate忽略的条目数
     */
    private Integer ignoredByVersionGate;

    /**
     * 当前软件清单版本
     */
    private Long currentInventoryVersion;

}
