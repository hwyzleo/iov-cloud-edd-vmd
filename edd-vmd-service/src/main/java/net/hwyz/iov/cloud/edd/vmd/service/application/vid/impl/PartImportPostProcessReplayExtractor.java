package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 零件导入后置处理候选实例提取器
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 * <p>
 * 从 part_import_data.data 原始报文只读提取并去重候选 (partCode, sn)。
 * <p>
 * 注意：本提取器不调用 ImportDataParserRegistry / 具体 *DataParser / 统一入站内核，
 * 不执行字段校验、type-schema 标准化或主体落库（D34）。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
@Component
public class PartImportPostProcessReplayExtractor {

    /**
     * 候选实例
     */
    public record Candidate(String partCode, String sn) {
    }

    /**
     * 从原始报文中提取去重后的候选实例列表
     * <p>
     * partCode 提取规则与通用导入一致：优先 ASSEMBLY_PART_NO，缺失时使用 HARDWARE_PART_NO。
     *
     * @param rawData 原始报文 JSON 字符串
     * @return 去重后的候选实例列表（保持原顺序）
     */
    public List<Candidate> extractDistinctCandidates(String rawData) {
        Set<String> keySet = new LinkedHashSet<>();
        List<Candidate> candidates = new ArrayList<>();
        if (StrUtil.isBlank(rawData)) {
            return candidates;
        }
        try {
            JSONObject dataJson = JSONUtil.parseObj(rawData);
            JSONObject data = getData(dataJson);
            if (data == null) {
                log.warn("原始报文缺少 DATA 部分");
                return candidates;
            }
            JSONArray items = data.getJSONArray("ITEMS");
            if (items == null || items.isEmpty()) {
                log.warn("原始报文缺少 ITEMS 部分或为空");
                return candidates;
            }
            for (Object item : items) {
                JSONObject itemJson = JSONUtil.parseObj(item);
                String sn = itemJson.getStr("SN");
                String assemblyPartNo = itemJson.getStr("ASSEMBLY_PART_NO");
                String hardwarePartNo = itemJson.getStr("HARDWARE_PART_NO");
                String partCode = StrUtil.isNotBlank(assemblyPartNo) ? assemblyPartNo : hardwarePartNo;
                if (StrUtil.isBlank(partCode) || StrUtil.isBlank(sn)) {
                    log.warn("原始报文存在空零件编码或SN的记录，跳过");
                    continue;
                }
                String key = partCode.trim() + ":" + sn.trim();
                if (keySet.add(key)) {
                    candidates.add(new Candidate(partCode.trim(), sn.trim()));
                }
            }
        } catch (Exception e) {
            log.error("提取候选实例失败: {}", e.getMessage(), e);
        }
        return candidates;
    }

    /**
     * 获取数据部分
     *
     * @param dataJson 整体数据JSON对象
     * @return 数据部分JSON对象
     */
    private JSONObject getData(JSONObject dataJson) {
        JSONObject request = dataJson.getJSONObject("REQUEST");
        if (request == null) {
            return null;
        }
        return request.getJSONObject("DATA");
    }
}
