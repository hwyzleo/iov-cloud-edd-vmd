package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehiclePublish;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import org.springframework.stereotype.Component;

/**
 * 车辆生产数据解析器V1.0
 *
 * @author hwyz_leo
 */
@Slf4j
@RequiredArgsConstructor
@Component("produceDataParserV1.0")
public class ProduceDataParserV1_0 extends BaseParser implements ImportDataParser {

    private final VehiclePublish vehiclePublish;
    private final VehBasicInfoRepository vehBasicInfoRepository;

    @Override
    public void parse(String batchNum, JSONObject dataJson) {
        JSONObject data = getData(dataJson);
        JSONArray items = data.getJSONArray("ITEMS");
        int vehicleInvalidCount = 0;
        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            String vin = itemJson.getStr("VIN");
            if (StrUtil.isBlank(vin)) {
                vehicleInvalidCount++;
                continue;
            }
            VehicleBasicInfo vehicleBasicInfo = vehBasicInfoRepository.selectByVin(vin);
            if (ObjUtil.isNull(vehicleBasicInfo)) {
                vehicleBasicInfo = VehicleBasicInfo.builder()
                        .vin(vin)
                        .build();
            }
            handleVehicleInfo(itemJson, vehicleBasicInfo, "MANUFACTURER", "manufacturerCode", "工厂数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehicleBasicInfo, "BRAND", "brandCode", "品牌数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehicleBasicInfo, "PLATFORM", "platformCode", "平台数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehicleBasicInfo, "SERIES", "seriesCode", "车系数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehicleBasicInfo, "MODEL", "modelCode", "车型数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehicleBasicInfo, "BASE_MODEL", "baseModelCode", "基础车型数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehicleBasicInfo, "BUILD_CONFIG", "buildConfigCode", "生产配置数据", batchNum, vin);
            if (ObjUtil.isNull(vehicleBasicInfo.getId())) {
                vehBasicInfoRepository.insert(vehicleBasicInfo);
            } else {
                vehBasicInfoRepository.update(vehicleBasicInfo);
            }
            // 发布事件
            vehiclePublish.produce(vin);
        }
        if (vehicleInvalidCount > 0) {
            log.warn("车辆生产导入数据批次号[{}]存在无效车辆数据[{}]", batchNum, vehicleInvalidCount);
        }
    }
}
