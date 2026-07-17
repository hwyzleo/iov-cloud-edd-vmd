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
 * PRODUCE 事件补发提取器
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发
 * <p>
 * 从 veh_import_data 原始报文只读提取 VIN 并标准化、去重
 * <p>
 * 注意：本提取器不调用 ImportDataParserRegistry / VehicleProduceParser，
 * 不重入 D15 六步内核，不重新建档/更新车辆、写选项值快照、绑定零件或触发安全常量预置
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Slf4j
@Component
public class ProduceEventReplayExtractor {

    /**
     * 从原始报文中提取去重后的 VIN 列表
     *
     * @param rawData 原始报文 JSON 字符串
     * @return 去重后的 VIN 列表
     */
    public List<String> extractDistinctVins(String rawData) {
        Set<String> vinSet = new LinkedHashSet<>();
        try {
            JSONObject dataJson = JSONUtil.parseObj(rawData);
            JSONObject data = getData(dataJson);
            if (data == null) {
                log.warn("原始报文缺少 DATA 部分");
                return new ArrayList<>();
            }
            JSONArray items = data.getJSONArray("ITEMS");
            if (items == null || items.isEmpty()) {
                log.warn("原始报文缺少 ITEMS 部分或为空");
                return new ArrayList<>();
            }
            for (Object item : items) {
                JSONObject itemJson = JSONUtil.parseObj(item);
                String vin = itemJson.getStr("VIN");
                if (StrUtil.isNotBlank(vin)) {
                    // 标准化：去除首尾空格，转大写
                    vin = vin.trim().toUpperCase();
                    vinSet.add(vin);
                }
            }
        } catch (Exception e) {
            log.error("提取 VIN 失败: {}", e.getMessage(), e);
        }
        return new ArrayList<>(vinSet);
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
