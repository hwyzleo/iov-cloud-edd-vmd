package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehiclePublish;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehBasicInfoMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBasicInfoPo;
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
    private final VehBasicInfoMapper vehBasicInfoMapper;
    private final VehicleAppService vehicleAppService;

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
            VehBasicInfoPo vehBasicInfoPo = vehBasicInfoMapper.selectPoByVin(vin);
            if (ObjUtil.isNull(vehBasicInfoPo)) {
                vehBasicInfoPo = new VehBasicInfoPo();
                vehBasicInfoPo.setVin(vin);
            }
            handleVehicleInfo(itemJson, vehBasicInfoPo, "MANUFACTURER", "manufacturerCode", "工厂数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehBasicInfoPo, "BRAND", "brandCode", "品牌数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehBasicInfoPo, "PLATFORM", "platformCode", "平台数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehBasicInfoPo, "SERIES", "seriesCode", "车系数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehBasicInfoPo, "MODEL", "modelCode", "车型数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehBasicInfoPo, "BASE_MODEL", "baseModelCode", "基础车型数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehBasicInfoPo, "BUILD_CONFIG", "buildConfigCode", "生产配置数据", batchNum, vin);
            if (ObjUtil.isNull(vehBasicInfoPo.getId())) {
                vehBasicInfoMapper.insertPo(vehBasicInfoPo);
            } else {
                vehBasicInfoMapper.updatePo(vehBasicInfoPo);
            }
            // 发布事件
            vehiclePublish.produce(vin);
        }
        if (vehicleInvalidCount > 0) {
            log.warn("车辆生产导入数据批次号[{}]存在无效车辆数据[{}]", batchNum, vehicleInvalidCount);
        }
    }
}
