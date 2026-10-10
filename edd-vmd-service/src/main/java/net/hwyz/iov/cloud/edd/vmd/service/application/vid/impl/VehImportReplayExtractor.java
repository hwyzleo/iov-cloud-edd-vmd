package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆导入补发候选提取器
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发扩展为按 ImportType 路由的动作补偿
 * <p>
 * 从 veh_import_data.data 原始报文只读提取并去重候选 VIN（PRODUCE/TOL/EOL），
 * TOL/EOL 同时提取每辆车的候选零件 (partCode, sn) 与 EOL 原批次时间。
 * <p>
 * 注意：本提取器不调用 ImportDataParserRegistry / 具体 *Parser / 六步内核，
 * 不执行字段校验或主体落库；原始报文仅负责候选集识别，payload 一律从当前事实构造。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
@Component
public class VehImportReplayExtractor {

    /**
     * 单辆车的候选信息
     *
     * @param vin      车架号（标准化大写）
     * @param eolTime  原批次 EOL 时间（仅 EOL 报文可提取，可空）
     * @param parts    候选零件列表（PRODUCE 为空）
     */
    public record VehCandidate(String vin, Instant eolTime, List<PartCandidate> parts) {
    }

    /**
     * 候选零件
     *
     * @param partCode 零件编码
     * @param sn       零件序列号
     */
    public record PartCandidate(String partCode, String sn) {
    }

    /**
     * 从原始报文中提取去重后的候选车辆列表（保持原顺序）
     *
     * @param rawData 原始报文 JSON 字符串
     * @param importType 导入类型（PRODUCE/TOL/EOL）
     * @return 候选车辆列表
     */
    public List<VehCandidate> extractCandidates(String rawData, String importType) {
        if (StrUtil.isBlank(rawData)) {
            return List.of();
        }
        try {
            JSONObject dataJson = JSONUtil.parseObj(rawData);
            JSONObject data = getData(dataJson);
            if (data == null) {
                log.warn("原始报文缺少 DATA 部分");
                return List.of();
            }
            JSONArray items = data.getJSONArray("ITEMS");
            if (items == null || items.isEmpty()) {
                log.warn("原始报文缺少 ITEMS 部分或为空");
                return List.of();
            }

            Map<String, VehCandidate> candidates = new LinkedHashMap<>();
            for (Object item : items) {
                JSONObject itemJson = JSONUtil.parseObj(item);
                String vin = itemJson.getStr("VIN");
                if (StrUtil.isBlank(vin)) {
                    log.warn("原始报文存在 VIN 为空的记录，跳过");
                    continue;
                }
                vin = vin.trim().toUpperCase();
                // 合并同一 VIN 的多条 ITEM（如多行零件）
                VehCandidate existing = candidates.get(vin);
                List<PartCandidate> parts = existing != null ? new ArrayList<>(existing.parts()) : new ArrayList<>();
                Instant eolTime = existing != null ? existing.eolTime() : null;

                JSONArray partsArray = itemJson.getJSONArray("PARTS");
                if (partsArray != null) {
                    for (Object part : partsArray) {
                        JSONObject partJson = JSONUtil.parseObj(part);
                        String partCode = extractPartCode(partJson, importType);
                        String sn = extractSn(partJson, importType);
                        if (StrUtil.isBlank(partCode) || StrUtil.isBlank(sn)) {
                            log.warn("原始报文存在空零件编码或SN的零件记录，跳过: vin={}", vin);
                            continue;
                        }
                        parts.add(new PartCandidate(partCode.trim(), sn.trim()));
                    }
                }

                // EOL 原批次时间：优先 EOL_TIME 毫秒时间戳，兼容 EOL_DATE yyyyMMdd
                Instant itemEolTime = extractEolTime(itemJson);
                if (itemEolTime != null && eolTime == null) {
                    eolTime = itemEolTime;
                }

                candidates.put(vin, new VehCandidate(vin, eolTime, parts));
            }
            return new ArrayList<>(candidates.values());
        } catch (Exception e) {
            log.error("提取候选车辆失败: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 提取零件编码（TOL 取 ASSEMBLY_PART_NO 优先、HARDWARE_PART_NO 兜底；EOL 取 PART_NO）
     */
    private String extractPartCode(JSONObject partJson, String importType) {
        if ("EOL".equals(importType)) {
            String partNo = partJson.getStr("PART_NO");
            if (StrUtil.isNotBlank(partNo)) {
                return partNo;
            }
            return partJson.getStr("HARDWARE_PN");
        }
        String assemblyPartNo = partJson.getStr("ASSEMBLY_PART_NO");
        if (StrUtil.isNotBlank(assemblyPartNo)) {
            return assemblyPartNo;
        }
        return partJson.getStr("HARDWARE_PART_NO");
    }

    /**
     * 提取零件序列号（TOL 取 SN；EOL 取 PART_SN）
     */
    private String extractSn(JSONObject partJson, String importType) {
        if ("EOL".equals(importType)) {
            return partJson.getStr("PART_SN");
        }
        return partJson.getStr("SN");
    }

    /**
     * 提取 EOL 原批次时间（毫秒时间戳优先，兼容 yyyyMMdd）
     */
    private Instant extractEolTime(JSONObject itemJson) {
        try {
            Long eolTimeTs = itemJson.getLong("EOL_TIME");
            if (eolTimeTs != null && eolTimeTs > 0) {
                return Instant.ofEpochMilli(eolTimeTs);
            }
            String eolDateStr = itemJson.getStr("EOL_DATE");
            if (StrUtil.isNotBlank(eolDateStr)) {
                return DateUtil.parse(eolDateStr, "yyyyMMdd").toInstant();
            }
        } catch (Exception e) {
            log.warn("解析EOL原批次时间失败: {}", e.getMessage());
        }
        return null;
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
