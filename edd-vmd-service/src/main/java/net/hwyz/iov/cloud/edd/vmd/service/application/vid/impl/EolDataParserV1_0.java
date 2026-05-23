package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleEolPartBoundEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehiclePublish;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleLifecycleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleDetail;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 车辆下线数据解析器V1.0
 * <p>
 * 薄编排层：提取 → 持久化 → 事件 → 零件绑定 → 事件。
 * 具体逻辑委托给 {@link VehicleInfoExtractor}、{@link VehicleInfoPersister}、{@link VehiclePartBinder}。
 *
 * @author hwyz_leo
 */
@Slf4j
@RequiredArgsConstructor
@Component("eolDataParserV1.0")
public class EolDataParserV1_0 extends BaseParser implements ImportDataParser {

    private final VehiclePublish vehiclePublish;
    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final VehicleInfoExtractor vehicleInfoExtractor;
    private final VehicleInfoPersister vehicleInfoPersister;
    private final VehiclePartBinder vehiclePartBinder;
    private final VehicleLifecycleAppService vehicleLifecycleAppService;
    private final ImportDataParserRegistry parserRegistry;

    @PostConstruct
    public void init() {
        parserRegistry.register(this);
    }

    @Override
    public String getType() {
        return "EOL";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

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
            // 1. 提取
            VehicleBasicInfo existingInfo = vehBasicInfoRepository.selectByVin(vin);
            Map<String, VehicleDetail> existingDetailMap = vehBasicInfoRepository.selectDetailByVin(vin).stream()
                    .collect(Collectors.toMap(VehicleDetail::getType, v -> v));

            VehicleBasicInfo basicInfo = vehicleInfoExtractor.extractBasicInfo(itemJson, existingInfo, batchNum, vin);
            List<VehicleDetail> details = vehicleInfoExtractor.extractDetails(itemJson, existingDetailMap, batchNum, vin);
            Instant eolDate = vehicleInfoExtractor.extractEolDate(itemJson);

            boolean firstEol = ObjUtil.isNull(basicInfo.getEolTime());
            if (firstEol) {
                basicInfo.setEolTime(eolDate);
            }

            // 2. 持久化
            boolean isNewVehicle = vehicleInfoPersister.persist(basicInfo, details);

            // 3. 发布车辆事件
            if (isNewVehicle) {
                vehiclePublish.produce(vin);
            }
            if (firstEol) {
                vehiclePublish.eol(vin, eolDate);
            }

            // 4. 合格证节点
            String certDateStr = vehicleInfoExtractor.extractCertDateStr(itemJson);
            if (StrUtil.isNotBlank(certDateStr)) {
                var certDate = DateUtil.parse(certDateStr, "yyyyMMdd");
                if (ObjUtil.isNotNull(certDate)) {
                    vehicleLifecycleAppService.recordCertificateNode(vin, certDate);
                }
            }

            // 5. 零件绑定
            JSONArray parts = itemJson.getJSONArray("PARTS");
            List<VehicleEolPartBoundEvent.PartMeta> partMetaList = vehiclePartBinder.bindParts(parts, vin, batchNum);
            if (!partMetaList.isEmpty()) {
                vehiclePublish.eolPartBound(vin, partMetaList);
            }
        }
        if (vehicleInvalidCount > 0) {
            log.warn("车辆生产导入数据批次号[{}]存在无效车辆数据[{}]", batchNum, vehicleInvalidCount);
        }
    }
}
