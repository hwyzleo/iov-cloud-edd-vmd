package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehiclePublish;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleSecurityPresetAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.VehicleImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 整车主档后台导入解析器 (US-040)
 * 处理 type=PRODUCE 的整车主档批量导入
 * <p>
 * VMD-DSN-CR-027: 车辆数据导入域独立化，解析器注册到独立命名空间
 *
 * @since CR-025
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleProduceDataParserV1_0 extends BaseProcessor implements VehicleImportDataParser {

    private final VehiclePublish vehiclePublish;
    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final ImportDataParserRegistry parserRegistry;
    private final VehicleSecurityPresetAppService vehicleSecurityPresetAppService;

    @PostConstruct
    public void init() {
        parserRegistry.register(this);
    }

    @Override
    public String getType() {
        return "PRODUCE";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public ImportResult parse(String batchNum, JSONObject dataJson) {
        log.info("US-040: 处理整车主档批量导入, batchNum={}", batchNum);

        JSONObject data = getData(dataJson);
        JSONArray items = data.getJSONArray("ITEMS");
        int totalCount = items.size();
        int successCount = 0;
        int failureCount = 0;
        int invalidCount = 0;
        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            String vin = itemJson.getStr("VIN");
            if (StrUtil.isBlank(vin)) {
                invalidCount++;
                continue;
            }
            try {
                VehicleBasicInfo vehicleBasicInfo = vehBasicInfoRepository.selectByVin(vin);
                if (ObjUtil.isNull(vehicleBasicInfo)) {
                    vehicleBasicInfo = VehicleBasicInfo.builder()
                            .vin(vin)
                            .build();
                }
                handleVehicleInfo(itemJson, vehicleBasicInfo, "PLANT", "plantCode", "工厂数据", batchNum, vin);
                handleVehicleInfo(itemJson, vehicleBasicInfo, "BRAND", "brandCode", "品牌数据", batchNum, vin);
                handleVehicleInfo(itemJson, vehicleBasicInfo, "PLATFORM", "platformCode", "平台数据", batchNum, vin);
                handleVehicleInfo(itemJson, vehicleBasicInfo, "CAR_LINE", "carLineCode", "车系数据", batchNum, vin);
                handleVehicleInfo(itemJson, vehicleBasicInfo, "MODEL", "modelCode", "车型数据", batchNum, vin);
                handleVehicleInfo(itemJson, vehicleBasicInfo, "VARIANT", "variantCode", "版本数据", batchNum, vin);
                handleVehicleInfo(itemJson, vehicleBasicInfo, "CONFIGURATION", "configurationCode", "配置数据", batchNum, vin);
                log.info("车辆[{}]数据设置完成: variantCode={}, configurationCode={}", vin, vehicleBasicInfo.getVariantCode(), vehicleBasicInfo.getConfigurationCode());
                if (ObjUtil.isNull(vehicleBasicInfo.getId())) {
                    log.info("车辆[{}]执行insert操作", vin);
                    vehBasicInfoRepository.insert(vehicleBasicInfo);
                } else {
                    log.info("车辆[{}]执行update操作, id={}", vin, vehicleBasicInfo.getId());
                    vehBasicInfoRepository.update(vehicleBasicInfo);
                }
                vehiclePublish.produce(vin);
                // 预置安全常量
                try {
                    vehicleSecurityPresetAppService.preset(vin, batchNum);
                } catch (Exception e) {
                    log.warn("车辆[{}]安全常量预置失败: {}", vin, e.getMessage());
                    // 安全常量预置失败不计入failureCount，因为它是后置步骤
                }
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.warn("车辆生产导入数据批次号[{}]车辆[{}]处理失败: {}", batchNum, vin, e.getMessage());
            }
        }
        if (invalidCount > 0) {
            log.warn("车辆生产导入数据批次号[{}]存在无效车辆数据[{}]", batchNum, invalidCount);
        }
        return ImportResult.builder()
                .totalCount(totalCount)
                .successCount(successCount)
                .failureCount(failureCount)
                .invalidCount(invalidCount)
                .build();
    }
}
