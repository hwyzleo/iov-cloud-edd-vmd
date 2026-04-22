package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehiclePublish;
import net.hwyz.iov.cloud.edd.vmd.service.application.DeviceAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.VehicleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.VehicleLifecycleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.VehiclePartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.framework.common.enums.DeviceItem;
import cn.hutool.core.date.DateUtil;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.ota.pota.api.contract.VehiclePartExService;
import net.hwyz.iov.cloud.ota.pota.api.contract.request.SaveVehiclePartsRequest;
import net.hwyz.iov.cloud.ota.pota.api.feign.service.ExVehiclePartService;
import net.hwyz.iov.cloud.otd.wms.api.contract.PreInboundOrderExService;
import net.hwyz.iov.cloud.otd.wms.api.contract.enums.WarehouseLevel;
//import net.hwyz.iov.cloud.otd.wms.api.feign.service.ExPreInboundOrderService;
import net.hwyz.iov.cloud.tsp.ccp.api.contract.VehicleCcpExService;
import net.hwyz.iov.cloud.tsp.ccp.api.feign.service.ExVehicleCcpService;
import net.hwyz.iov.cloud.tsp.idcm.api.contract.VehicleIdcmExService;
import net.hwyz.iov.cloud.tsp.idcm.api.feign.service.ExVehicleIdcmService;
import net.hwyz.iov.cloud.tsp.mno.api.contract.VehicleNetworkExService;
import net.hwyz.iov.cloud.tsp.mno.api.feign.service.ExVehicleNetworkService;
import net.hwyz.iov.cloud.tsp.tbox.api.contract.VehicleTboxExService;
import net.hwyz.iov.cloud.tsp.tbox.api.feign.service.ExVehicleTboxService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.VehicleLifecycleNodeEnum;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehBasicInfoDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehDetailInfoDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdDeviceDo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBasicInfoDo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehDetailInfoDo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehiclePartDo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 车辆生产数据解析器V1.0
 *
 * @author hwyz_leo
 */
@Slf4j
@RequiredArgsConstructor
@Component("eolDataParserV1.0")
public class EolDataParserV1_0 extends BaseParser implements ImportDataParser {

    private final VehiclePublish vehiclePublish;
    private final VehBasicInfoDao vehBasicInfoDao;
    private final DeviceAppService deviceAppService;
    private final VehDetailInfoDao vehDetailInfoDao;
    private final VehicleAppService vehicleAppService;
    private final ExVehicleCcpService exVehicleCcpService;
    private final ExVehiclePartService exVehiclePartService;
    private final ExVehicleTboxService exVehicleTboxService;
    private final ExVehicleIdcmService exVehicleIdcmService;
    private final VehiclePartAppService vehiclePartAppService;
    private final ExVehicleNetworkService exVehicleNetworkService;
//    private final ExPreInboundOrderService exPreInboundOrderService;
    private final VehicleLifecycleAppService vehicleLifecycleAppService;

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
            VmdVehBasicInfoDo vehBasicInfoPo = vehicleAppService.getVehicleByVin(vin);
            Map<String, VmdVehDetailInfoDo> vehicleDetailMap = vehicleAppService.getVehicleDetailByVin(vin).stream().collect(Collectors.toMap(VmdVehDetailInfoDo::getType, v -> v));
            if (ObjUtil.isNull(vehBasicInfoPo)) {
                vehBasicInfoPo = new VmdVehBasicInfoDo();
                vehBasicInfoPo.setVin(vin);
            }
            handleVehicleInfo(itemJson, vehBasicInfoPo, "MANUFACTURER", "manufacturerCode", "工厂数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehBasicInfoPo, "BRAND", "brandCode", "品牌数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehBasicInfoPo, "PLATFORM", "platformCode", "平台数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehBasicInfoPo, "SERIES", "seriesCode", "车系数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehBasicInfoPo, "MODEL", "modelCode", "车型数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehBasicInfoPo, "BASE_MODEL", "baseModelCode", "基础车型数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehBasicInfoPo, "BUILD_CONFIG", "buildConfigCode", "生产配置数据", batchNum, vin);
            handleVehicleInfo(itemJson, vehBasicInfoPo, "VEHICLE_BASE_VERSION", "vehicleBaseVersion", "车辆基线版本", batchNum, vin);
            String eolDateStr = itemJson.getStr("EOL_DATE");
            DateTime eolDate;
            boolean firstEol = false;
            if (StrUtil.isNotBlank(eolDateStr)) {
                eolDate = DateUtil.parse(eolDateStr, "yyyyMMdd");
            } else {
                eolDate = new DateTime();
            }
            if (ObjUtil.isNull(vehBasicInfoPo.getEolTime())) {
                vehBasicInfoPo.setEolTime(eolDate);
                firstEol = true;
            } else if (eolDate.isBefore(vehBasicInfoPo.getEolTime()) || eolDate.isAfter(vehBasicInfoPo.getEolTime())) {
                log.warn("车辆导入数据批次号[{}]下线日期数据[{}]与原数据[{}]不一致", batchNum, eolDateStr, DateUtil.formatDate(vehBasicInfoPo.getEolTime()));
            }
            handleVehicleDetail(itemJson, vehicleDetailMap, "PRODUCTION_ORDER", "生产订单", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "MATNR", "整车物料编码", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "PROJECT", "车型项目", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "SALES_AREA", "销售区域", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "BODY_TYPE", "车身形式", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "CONFIG_LEVEL", "配置等级", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "MODEL_YEAR", "车型年份", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "STEERING_POSITION", "左右舵", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "INTERIOR_STYLE", "内饰风格", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "EXTERIOR_COLOR", "外饰颜色", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "DRIVE_TYPE", "驾驶形式", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "WHEEL", "轮毂", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "TIRE", "轮胎", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "SEAT_TYPE", "座椅类型", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "ASSISTED_DRIVING", "辅助驾驶", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "ETC_SYSTEM", "ETC系统", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "REAR_TOW_BAR", "后牵引杆", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "ENGINE_NO", "发动机编码", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "ENGINE_TYPE", "发动机类型", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "FRONT_DRIVE_MOTOR_NO", "前驱电机编码", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "FRONT_DRIVE_MOTOR_TYPE", "前驱电机类型", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "REAR_DRIVE_MOTOR_NO", "后驱电机编码", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "REAR_DRIVE_MOTOR_TYPE", "后驱电机类型", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "GENERATOR_NO", "发电机编码", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "GENERATOR_TYPE", "发电机类型", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "POWER_BATTERY_PACK_NO", "动力电池包编码", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "POWER_BATTERY_TYPE", "动力电池类型", batchNum, vin);
            handleVehicleDetail(itemJson, vehicleDetailMap, "POWER_BATTERY_FACTORY", "动力电池厂商", batchNum, vin);
            if (ObjUtil.isNull(vehBasicInfoPo.getId())) {
                vehBasicInfoDao.insertPo(vehBasicInfoPo);
                // 如果车辆是新生成，则补发车辆生产事件
                vehiclePublish.produce(vin);
            } else {
                vehBasicInfoDao.updatePo(vehBasicInfoPo);
            }
            List<VmdVehDetailInfoDo> needInsertDetailList = vehicleDetailMap.values().stream().filter(po -> po.getId() == null).toList();
            if (!needInsertDetailList.isEmpty()) {
                vehDetailInfoDao.batchInsertPo(needInsertDetailList);
            }
            if (firstEol) {
                vehiclePublish.eol(vin, eolDate);
            }
            String certDateStr = itemJson.getStr("CERT_DATE");
            if (StrUtil.isNotBlank(certDateStr) && ObjUtil.isNull(vehicleLifecycleAppService.getLifecycle(vin, VehicleLifecycleNodeEnum.CERTIFICATE))) {
                DateTime certDate = DateUtil.parse(certDateStr, "yyyyMMdd");
                if (ObjUtil.isNotNull(certDate)) {
                    vehicleLifecycleAppService.recordCertificateNode(vin, certDate);
                }
            }
            JSONArray parts = itemJson.getJSONArray("PARTS");
            SaveVehiclePartsRequest request = new SaveVehiclePartsRequest();
            request.setVin(vin);
            request.setRemark("车辆下线");
            List<VehiclePartExService> vehiclePartList = new ArrayList<>();
            for (Object part : parts) {
                JSONObject partJson = JSONUtil.parseObj(part);
                String deviceCode = partJson.getStr("DEVICE_CODE");
                if (StrUtil.isBlank(deviceCode)) {
                    log.warn("车辆导入数据批次号[{}]车架号[{}]设备[{}]为空", batchNum, vin, deviceCode);
                    continue;
                }
                String partVin = partJson.getStr("VIN");
                if (!vin.equalsIgnoreCase(partVin)) {
                    log.warn("车辆导入数据批次号[{}]车架号[{}]设备[{}]车架号[{}]不一致", batchNum, vin, deviceCode, partVin);
                    continue;
                }
                String pn = partJson.getStr("PART_NO");
                String sn = partJson.getStr("PART_SN");
                VmdDeviceDo device = deviceAppService.getDeviceByCode(deviceCode);
                String supplierCode = partJson.getStr("SUPPLIER_CODE");
                String configWord = partJson.getStr("CONFIG_WORD");
                String hardwareVersion = partJson.getStr("HARDWARE_VERSION");
                String softwareVersion = partJson.getStr("SOFTWARE_VERSION");
                String hardwarePn = partJson.getStr("HARDWARE_PN");
                String softwarePn = partJson.getStr("SOFTWARE_PN");
                if (ObjUtil.isNull(device)) {
                    log.warn("车辆导入数据批次号[{}]车架号[{}]设备[{}]异常", batchNum, vin, deviceCode);
                }
                vehiclePartList.add(VehiclePartExService.builder()
                        .sn(sn)
                        .pn(pn)
                        .deviceCode(deviceCode)
                        .deviceItem(device.getDeviceItem())
                        .supplierCode(supplierCode)
                        .batchNum(batchNum)
                        .configWord(configWord)
                        .hardwareVer(hardwareVersion)
                        .softwareVer(softwareVersion)
                        .hardwarePn(hardwarePn)
                        .softwarePn(softwarePn)
                        .build());
                try {
                    vehiclePartAppService.bindVehiclePart(VmdVehiclePartDo.builder()
                            .pn(pn)
                            .sn(sn)
                            .vin(vin)
                            .deviceCode(deviceCode)
                            .deviceItem(device.getDeviceItem())
                            .supplierCode(supplierCode)
                            .batchNum(batchNum)
                            .configWord(configWord)
                            .hardwareVer(hardwareVersion)
                            .softwareVer(softwareVersion)
                            .hardwarePn(hardwarePn)
                            .softwarePn(softwarePn)
                            .bindOrg("MES")
                            .build());
                } catch (Exception e) {
                    log.warn("车辆导入数据批次号[{}]车架号[{}]零部件[{}]绑定异常", batchNum, vin, deviceCode, e);
                }
                if (DeviceItem.TBOX.name().equalsIgnoreCase(device.getDeviceItem())) {
                    String iccid1 = partJson.getStr("ICCID1");
                    String iccid2 = partJson.getStr("ICCID2");
                    if (StrUtil.isNotBlank(iccid1)) {
                        exVehicleNetworkService.create(VehicleNetworkExService.builder()
                                .vin(vin)
                                .iccid1(iccid1)
                                .iccid2(iccid2)
                                .build());
                    }
                    exVehicleTboxService.bind(VehicleTboxExService.builder().vin(vin).sn(sn).build());
                }
                if (DeviceItem.CCP.name().equalsIgnoreCase(device.getDeviceItem())) {
                    exVehicleCcpService.bind(VehicleCcpExService.builder().vin(vin).sn(sn).build());
                }
                if (DeviceItem.IDCM.name().equalsIgnoreCase(device.getDeviceItem())) {
                    exVehicleIdcmService.bind(VehicleIdcmExService.builder().vin(vin).sn(sn).build());
                }
            }
            request.setVehiclePartList(vehiclePartList);
            exVehiclePartService.saveVehicleParts(vin, request);
            // 预期下线后1天内到达前置库，2小时内入库
//            exPreInboundOrderService.createOrder(PreInboundOrderExService.builder()
//                    .vin(vin)
//                    .buildConfigCode(itemJson.getStr("BUILD_CONFIG"))
//                    .warehouseLevel(WarehouseLevel.PDC.name())
//                    .estimatedArrivalTime(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
//                    .estimatedInboundTime(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000 + 2 * 60 * 60 * 1000))
//                    .build());
        }
        if (vehicleInvalidCount > 0) {
            log.warn("车辆生产导入数据批次号[{}]存在无效车辆数据[{}]", batchNum, vehicleInvalidCount);
        }
    }
}
