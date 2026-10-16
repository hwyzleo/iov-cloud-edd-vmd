package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import org.springframework.stereotype.Service;

/**
 * 零件编码解析器（零件导入统一口径）
 * <p>
 * 零件导入报文中同时存在两种编码：
 * <ul>
 *   <li>{@code ASSEMBLY_PART_NO}：装车软硬件总成号 / 装车总成零件号；</li>
 *   <li>{@code HARDWARE_PART_NO}：硬件总成号。</li>
 * </ul>
 * 按 CR-041 收口，{@code part_info.part_code} 语义为总成零件号，故统一解析规则为
 * <b>优先 {@code ASSEMBLY_PART_NO}，缺失时回退 {@code HARDWARE_PART_NO}</b>。
 * <p>
 * 该规则必须被以下写/读路径共用，避免同一物理实例在不同表中落到不同 part_code：
 * <ul>
 *   <li>通用导入落 {@code part_info.part_code}（PartImportDataAppService）；</li>
 *   <li>器件级安全常量预置落 {@code part_security_constant.part_code}（与 part_info 同源）；</li>
 *   <li>零件导入后置处理重放候选提取（PartImportPostProcessReplayExtractor）。</li>
 * </ul>
 * 仅返回原始值，不做 trim/大小写转换，保持与 part_info 落库口径一致；空值/缺字段返回 null。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
@Service
public class PartCodeResolver {

    /**
     * 装车软硬件总成号字段
     */
    public static final String FIELD_ASSEMBLY_PART_NO = "ASSEMBLY_PART_NO";

    /**
     * 硬件总成号字段
     */
    public static final String FIELD_HARDWARE_PART_NO = "HARDWARE_PART_NO";

    /**
     * 从导入报文的单个 ITEM 解析零件编码：优先总成零件号，缺失回退硬件总成号。
     *
     * @param itemJson 导入报文 ITEM
     * @return 零件编码，两者均为空时返回 null
     */
    public String resolve(JSONObject itemJson) {
        if (itemJson == null) {
            return null;
        }
        String assemblyPartNo = itemJson.getStr(FIELD_ASSEMBLY_PART_NO);
        if (StrUtil.isNotBlank(assemblyPartNo)) {
            return assemblyPartNo;
        }
        return itemJson.getStr(FIELD_HARDWARE_PART_NO);
    }
}
