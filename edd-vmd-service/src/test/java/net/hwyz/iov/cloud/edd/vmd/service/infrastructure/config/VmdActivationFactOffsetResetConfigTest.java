package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 激活事实消费「位点策略」配置断言（VMD-DSN-CR-077 终审 R-8）
 * <p>
 * 背景：原 {@code auto-offset-reset=latest} 在首次开启消费时跳过 Topic 中既有事实，
 * 导致「早期已激活、事实在开启前已投递」的车辆永远缺首次激活节点（查询恒为
 * {@code NOT_RECORDED}）。终审 R-8 定稿改为 {@code earliest}：
 * <ul>
 *   <li>{@code earliest} 仅在<strong>新消费组首次分配分区</strong>时被 Kafka 采用，
 *       已有 committed offset 的消费组不受影响（不会重复回放）</li>
 *   <li>开启前用 {@code earliest} 回放历史事实；回放完成后（lag 归零、台账无新增
 *       {@code RECORDED}）如需可切回 {@code latest} 仅消费增量</li>
 * </ul>
 * 本测试对「类型化配置默认值 + 生效 yml（非注释行）」双重断言，防止回潮为 {@code latest}；
 * 同时断言 yml 注释保留了「earliest 回放 → 可切回 latest」的运维说明，
 * 以及生产默认仍为消费关闭（{@code enabled=false}）。
 *
 * @author hwyz_leo
 */
@DisplayName("激活事实消费位点策略配置断言（终审 R-8）")
class VmdActivationFactOffsetResetConfigTest {

    /**
     * 生效配置中的位点策略键（含环境变量占位符与默认值）
     */
    private static final String OFFSET_RESET_KEY = "auto-offset-reset:";

    /**
     * 生效配置行：默认 earliest
     */
    private static final String EXPECTED_OFFSET_RESET =
            "auto-offset-reset: ${VMD_ACTIVATION_FACT_KAFKA_AUTO_OFFSET_RESET:earliest}";

    @Test
    @DisplayName("类型化配置默认 auto-offset-reset=earliest（终审 R-8）")
    void propertiesDefaultIsEarliest() {
        VmdActivationFactConsumerProperties properties = new VmdActivationFactConsumerProperties();

        assertEquals("earliest", properties.getAutoOffsetReset(),
                "默认必须为 earliest：开启消费时回放历史事实，补齐早期已激活车辆的首次激活节点");
        assertFalse(properties.isEnabled(), "生产默认仍为消费关闭（暗部署，终审 R-8 未改变）");
        assertEquals("edd-vmd-vehicle-activation-fact", properties.getGroupId(),
                "消费组 ID 不得变更（换组会丢失位点语义，回放范围不可控）");
    }

    @Test
    @DisplayName("application.yml 生效行：默认 earliest + 生产默认消费关闭")
    void yamlDefaultsToEarliest() throws IOException {
        List<String> activeLines = activeLines();

        assertTrue(activeLines.stream().anyMatch(line -> line.equals(EXPECTED_OFFSET_RESET)),
                "application.yml 生效行必须为 '" + EXPECTED_OFFSET_RESET + "'，实际生效行：" + activeLines);
        assertFalse(activeLines.stream().anyMatch(
                        line -> line.startsWith(OFFSET_RESET_KEY) && line.endsWith(":latest}")),
                "application.yml 生效行不得回潮为 latest");
        assertTrue(activeLines.stream().anyMatch(
                        line -> line.equals("enabled: ${VMD_ACTIVATION_FACT_KAFKA_ENABLED:false}")),
                "生产默认仍为消费关闭（enabled=false）");
    }

    @Test
    @DisplayName("application.yml 运维说明：earliest 回放历史事实 + 回放完成后可切回 latest")
    void yamlDocumentsReplayStrategy() throws IOException {
        String yml = Files.readString(Paths.get("src/main/resources/application.yml"));

        assertTrue(yml.contains("earliest") && yml.contains("回放"),
                "配置注释必须写明「开启前用 earliest 回放历史事实」");
        assertTrue(yml.contains("切回 latest"),
                "配置注释必须写明「回放完成后如需可切回 latest」");
        assertTrue(yml.contains("已有位点不受影响") || yml.contains("仅对"),
                "配置注释必须写明 earliest 仅对新消费组首次生效、已有位点不受影响");
    }

    /**
     * 读取生效配置行（剔除注释与空行）。
     *
     * @return 生效配置行（已 trim）
     * @throws IOException 读文件失败
     */
    private List<String> activeLines() throws IOException {
        Path yml = Paths.get("src/main/resources/application.yml");
        List<String> active = new ArrayList<>();
        for (String line : Files.readAllLines(yml)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            active.add(trimmed);
        }
        return active;
    }

}
