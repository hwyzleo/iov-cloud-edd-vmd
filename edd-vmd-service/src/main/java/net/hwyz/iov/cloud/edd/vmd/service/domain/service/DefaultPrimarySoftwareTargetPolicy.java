package net.hwyz.iov.cloud.edd.vmd.service.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 默认主软件 Target 策略（CR-071）
 * <p>
 * 按 SOFTWARE_TYPE 优先级判定主软件，配置为全局有序类型列表（不写死零件号）：
 * <pre>
 * vmd:
 *   software-inventory:
 *     snapshot:
 *       primary-software-types:
 *         - APP
 *         - CALIBRATION
 *         - BOOT
 * </pre>
 * 列表顺序即优先级（0 最主）；未在列表中的类型返回 -1（不参与主选，投影置 AMBIGUOUS）。
 * 策略版本受控：配置变更经 Nacos/配置发布，不硬编码在解析器。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Component
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "vmd.software-inventory.snapshot")
public class DefaultPrimarySoftwareTargetPolicy implements PrimarySoftwareTargetPolicy {

    /** 主软件类型优先级（列表顺序 = 优先级，0 最主） */
    private List<String> primarySoftwareTypes = List.of();

    @Override
    public int priorityOf(String softwareType) {
        if (softwareType == null || softwareType.isBlank()) {
            return -1;
        }
        int idx = primarySoftwareTypes.indexOf(softwareType);
        return idx >= 0 ? idx : -1;
    }

    public void setPrimarySoftwareTypes(List<String> primarySoftwareTypes) {
        if (primarySoftwareTypes != null) {
            this.primarySoftwareTypes = primarySoftwareTypes;
        }
    }

    public List<String> getPrimarySoftwareTypes() {
        return primarySoftwareTypes;
    }

}
