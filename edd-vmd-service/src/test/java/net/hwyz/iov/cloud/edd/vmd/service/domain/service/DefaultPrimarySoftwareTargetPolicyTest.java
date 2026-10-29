package net.hwyz.iov.cloud.edd.vmd.service.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 默认主软件 Target 策略单元测试（CR-071 §5.1：按 SOFTWARE_TYPE 优先级）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
class DefaultPrimarySoftwareTargetPolicyTest {

    @Test
    @DisplayName("配置类型按列表顺序返回优先级（0 最主）")
    void priority_orderByList() {
        DefaultPrimarySoftwareTargetPolicy policy = new DefaultPrimarySoftwareTargetPolicy();
        policy.setPrimarySoftwareTypes(List.of("APP", "CALIBRATION", "BOOT"));

        assertThat(policy.priorityOf("APP")).isEqualTo(0);
        assertThat(policy.priorityOf("CALIBRATION")).isEqualTo(1);
        assertThat(policy.priorityOf("BOOT")).isEqualTo(2);
    }

    @Test
    @DisplayName("未配置类型返回 -1（不参与主选）")
    void priority_unmapped_minusOne() {
        DefaultPrimarySoftwareTargetPolicy policy = new DefaultPrimarySoftwareTargetPolicy();
        policy.setPrimarySoftwareTypes(List.of("APP"));

        assertThat(policy.priorityOf("BOOT")).isEqualTo(-1);
        assertThat(policy.priorityOf("UNKNOWN")).isEqualTo(-1);
    }

    @Test
    @DisplayName("空类型/未配置列表返回 -1")
    void priority_blank_minusOne() {
        DefaultPrimarySoftwareTargetPolicy policy = new DefaultPrimarySoftwareTargetPolicy();

        assertThat(policy.priorityOf(null)).isEqualTo(-1);
        assertThat(policy.priorityOf("")).isEqualTo(-1);
        assertThat(policy.priorityOf("APP")).isEqualTo(-1); // 列表未配置
    }

}
