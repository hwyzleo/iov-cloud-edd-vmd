package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 零件类型字段契约（type-schema）
 * <p>
 * 定义该类型必需/可选的特殊字段
 * 由入站内核在校验/标准化阶段消费
 *
 * @author hwyz_leo
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartTypeSchema {

    /**
     * 零件类型
     */
    private PartType partType;

    /**
     * 必需特殊字段列表
     */
    private List<String> requiredFields;

    /**
     * 可选特殊字段列表
     */
    private List<String> optionalFields;

    /**
     * 默认车载节点代码（可选，如BTM默认BTM_M）
     */
    private String defaultVehicleNodeCode;

    /**
     * 默认设备项（安装位置）
     */
    private String defaultDeviceItem;

    /**
     * 标准化特殊字段
     * <p>
     * 将异构特殊字段标准化为extra JSON格式
     *
     * @param rawFields 原始字段
     * @return 标准化后的extra JSON字符串
     */
    public String normalizeExtra(Map<String, Object> rawFields) {
        if (rawFields == null || rawFields.isEmpty()) {
            return null;
        }
        // 过滤掉null值
        Map<String, Object> filtered = new java.util.HashMap<>();
        rawFields.forEach((k, v) -> {
            if (v != null && !v.toString().isBlank()) {
                filtered.put(k, v);
            }
        });
        return filtered.isEmpty() ? null : cn.hutool.json.JSONUtil.toJsonStr(filtered);
    }

    /**
     * 校验必需字段是否齐全
     *
     * @param fields 字段Map
     * @return 缺失的必需字段列表
     */
    public List<String> validateRequired(Map<String, String> fields) {
        if (requiredFields == null || requiredFields.isEmpty()) {
            return List.of();
        }
        return requiredFields.stream()
                .filter(field -> !fields.containsKey(field) || fields.get(field) == null || fields.get(field).isBlank())
                .toList();
    }
}
