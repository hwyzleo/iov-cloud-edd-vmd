package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleEolPartBoundEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehiclePublish;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.EolResultGateService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.SecurityProvisionConfirmService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.SoftwareInventoryAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleLifecycleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleSecurityPresetAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.VehicleImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleDetail;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.Date;
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
public class EolDataParserV1_0 extends BaseProcessor implements VehicleImportDataParser {

    private final VehiclePublish vehiclePublish;
    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final VehicleInfoExtractor vehicleInfoExtractor;
    private final VehicleInfoPersister vehicleInfoPersister;
    private final VehiclePartBinder vehiclePartBinder;
    private final VehicleLifecycleAppService vehicleLifecycleAppService;
    private final VehicleSecurityPresetAppService vehicleSecurityPresetAppService;
    private final ImportDataParserRegistry parserRegistry;
    private final EolResultGateService eolResultGateService;
    private final SoftwareInventoryAppService softwareInventoryAppService;
    private final SecurityProvisionConfirmService securityProvisionConfirmService;
    private final VehiclePartAppService vehiclePartAppService;

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
    public ImportResult parse(String batchNum, JSONObject dataJson) {
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
                // CR-043: 结构化解析流程
                processStructuredEolData(batchNum, vin, itemJson);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.warn("车辆下线导入数据批次号[{}]车辆[{}]处理失败: {}", batchNum, vin, e.getMessage());
            }
        }
        if (invalidCount > 0) {
            log.warn("车辆下线导入数据批次号[{}]存在无效车辆数据[{}]", batchNum, invalidCount);
        }
        return ImportResult.builder()
                .totalCount(totalCount)
                .successCount(successCount)
                .failureCount(failureCount)
                .invalidCount(invalidCount)
                .build();
    }

    /**
     * CR-043: 处理结构化EOL数据
     * <p>
     * 流程：结构化解析→EOL_RESULT门禁→软件实装回写→安全回执对账→生命周期节点→补偿绑定→按域转发
     */
    private void processStructuredEolData(String batchNum, String vin, JSONObject itemJson) {
        // 1. 结构化解析（CERTIFICATE/OTA_BASELINE/ECU_BASELINE[]/INSPECTION_ITEMS[]/DIAGNOSTIC/POWERTRAIN）
        JSONObject certificate = itemJson.getJSONObject("CERTIFICATE");
        JSONObject otaBaseline = itemJson.getJSONObject("OTA_BASELINE");
        JSONArray ecuBaseline = itemJson.getJSONArray("ECU_BASELINE");
        JSONArray inspectionItems = itemJson.getJSONArray("INSPECTION_ITEMS");
        JSONObject diagnostic = itemJson.getJSONObject("DIAGNOSTIC");
        JSONObject powertrain = itemJson.getJSONObject("POWERTRAIN");

        // 2. 提取基础信息和详情（兼容旧格式）
        VehicleBasicInfo existingInfo = vehBasicInfoRepository.selectByVin(vin);
        Map<String, VehicleDetail> existingDetailMap = vehBasicInfoRepository.selectDetailByVin(vin).stream()
                .collect(Collectors.toMap(VehicleDetail::getType, v -> v));

        VehicleBasicInfo basicInfo;
        // VIN 不存在时 EOL 仍自动建车兜底（输出 WARN、残档）
        if (existingInfo == null) {
            log.warn("车辆[{}]不存在，EOL 自动建车兜底（残档，缺七项生产配置与选项值快照）", vin);
            basicInfo = vehicleInfoExtractor.createStubVehicle(itemJson, batchNum, vin);
        } else {
            basicInfo = vehicleInfoExtractor.extractBasicInfo(itemJson, existingInfo, batchNum, vin);
        }
        List<VehicleDetail> details = vehicleInfoExtractor.extractDetails(itemJson, existingDetailMap, batchNum, vin);
        
        // EOL_TIME 是时间戳格式（毫秒）
        Instant eolDate = extractEolTime(itemJson);

        boolean firstEol = ObjUtil.isNull(basicInfo.getEolTime());
        if (firstEol) {
            basicInfo.setEolTime(eolDate);
        }

        // 3. 持久化
        boolean isNewVehicle = vehicleInfoPersister.persist(basicInfo, details);

        // 4. EOL_RESULT放行门禁（CR-043）
        String eolResult = itemJson.getStr("EOL_RESULT");
        if (StrUtil.isNotBlank(eolResult)) {
            eolResultGateService.processEolResult(vin, eolResult);
        }

        // 5. 软件实装回写（CR-041/CR-043）
        processSoftwareInventory(batchNum, vin, ecuBaseline);

        // 6. 安全回执对账（CR-043）
        processSecurityProvisionConfirm(vin, ecuBaseline);

        // 7. 生命周期节点
        if (isNewVehicle) {
            // EOL 补发的 PRODUCE 事件，标记为 EOL- 前缀
            vehiclePublish.produce(vin, "EOL-" + batchNum);
        }
        if (firstEol) {
            vehiclePublish.eol(vin, eolDate);
        }

        // 8. 合格证节点（从CERTIFICATE子对象提取）
        if (certificate != null) {
            Long certDateTs = certificate.getLong("CERT_DATE");
            if (certDateTs != null && certDateTs > 0) {
                Instant certDate = Instant.ofEpochMilli(certDateTs);
                vehicleLifecycleAppService.recordCertificateNode(vin, Date.from(certDate));
            }
        }

        // 9. POWER_DOWN节点（CR-043）- 时间戳格式
        processPowerDownNode(vin, itemJson);

        // 10. 按域转发（CR-043）
        processDomainForwarding(vin, inspectionItems, diagnostic, otaBaseline, powertrain);
        
        // 11. 记录生产元数据（新增字段）
        logProductionMetadata(vin, itemJson);
    }

    /**
     * 提取EOL时间（时间戳格式）
     */
    private Instant extractEolTime(JSONObject itemJson) {
        Long eolTimeTs = itemJson.getLong("EOL_TIME");
        if (eolTimeTs != null && eolTimeTs > 0) {
            return Instant.ofEpochMilli(eolTimeTs);
        }
        // 兼容旧格式 EOL_DATE (yyyyMMdd)
        String eolDateStr = itemJson.getStr("EOL_DATE");
        if (StrUtil.isNotBlank(eolDateStr)) {
            return DateUtil.parse(eolDateStr, "yyyyMMdd").toInstant();
        }
        return Instant.now();
    }

    /**
     * 记录生产元数据（新增字段）
     */
    private void logProductionMetadata(String vin, JSONObject itemJson) {
        String plant = itemJson.getStr("PLANT");
        String lineCode = itemJson.getStr("LINE_CODE");
        String stationCode = itemJson.getStr("STATION_CODE");
        String shift = itemJson.getStr("SHIFT");
        String operator = itemJson.getStr("OPERATOR");
        String transportMode = itemJson.getStr("TRANSPORT_MODE");
        Integer odometerKm = itemJson.getInt("ODOMETER_KM");
        Integer soc = itemJson.getInt("SOC");
        String hvStatus = itemJson.getStr("HV_STATUS");
        
        log.debug("车辆[{}]生产元数据: 工厂={}, 产线={}, 工位={}, 班次={}, 操作员={}, 运输模式={}, 里程={}, SOC={}, 高压状态={}",
                vin, plant, lineCode, stationCode, shift, operator, transportMode, odometerKm, soc, hvStatus);
        
        // TODO: 可扩展存储到 vehicle_production_metadata 表或写入详情
    }

    /**
     * 处理软件实装回写
     * <p>
     * 新格式ECU_BASELINE字段映射：
     * - SN (原ECU_SN)
     * - ASSEMBLY_PART_NO (原PART_CODE)
     * - VEHICLE_NODE
     * - DEVICE_ITEM
     * - HARDWARE_PART_NO
     * - HARDWARE_VERSION
     * - SOFTWARE[]: SOFTWARE_TYPE, SOFTWARE_PART_NO, SOFTWARE_VERSION, FLASH_RESULT
     */
    private void processSoftwareInventory(String batchNum, String vin, JSONArray ecuBaseline) {
        if (ecuBaseline == null || ecuBaseline.isEmpty()) {
            return;
        }

        for (Object ecuObj : ecuBaseline) {
            JSONObject ecu = JSONUtil.parseObj(ecuObj);
            // 新格式字段映射
            String ecuSn = ecu.getStr("SN");
            String assemblyPartNo = ecu.getStr("ASSEMBLY_PART_NO");
            String vehicleNode = ecu.getStr("VEHICLE_NODE");
            String deviceItem = ecu.getStr("DEVICE_ITEM");
            String hardwarePartNo = ecu.getStr("HARDWARE_PART_NO");
            String hardwareVersion = ecu.getStr("HARDWARE_VERSION");
            
            if (StrUtil.isBlank(ecuSn) || StrUtil.isBlank(assemblyPartNo)) {
                log.debug("ECU SN或装配零件号为空，跳过: vin={}, vehicleNode={}", vin, vehicleNode);
                continue;
            }

            // 根据VIN和vehicleNode获取绑定的零件信息
            VehiclePart vehiclePart = vehiclePartAppService.findByVinAndPosition(vin, vehicleNode);
            if (vehiclePart == null || vehiclePart.getPartId() == null) {
                log.warn("车辆[{}]位置[{}]未找到绑定零件，跳过软件实装回写", vin, vehicleNode);
                continue;
            }
            Long partId = vehiclePart.getPartId();
            Long bindingId = vehiclePart.getId();

            log.debug("处理ECU基线: vin={}, vehicleNode={}, deviceItem={}, sn={}, assemblyPartNo={}, hwPartNo={}, hwVer={}, partId={}",
                    vin, vehicleNode, deviceItem, ecuSn, assemblyPartNo, hardwarePartNo, hardwareVersion, partId);

            // 处理软件清单
            JSONArray softwareList = ecu.getJSONArray("SOFTWARE");
            if (softwareList != null && !softwareList.isEmpty()) {
                for (Object swObj : softwareList) {
                    JSONObject sw = JSONUtil.parseObj(swObj);
                    String softwareType = sw.getStr("SOFTWARE_TYPE");
                    String softwarePartNo = sw.getStr("SOFTWARE_PART_NO");
                    String softwareVersion = sw.getStr("SOFTWARE_VERSION");
                    String flashResult = sw.getStr("FLASH_RESULT");

                    if (StrUtil.isNotBlank(softwarePartNo) && StrUtil.isNotBlank(softwareVersion)) {
                        log.debug("处理软件实装: vin={}, ecuSn={}, vehicleNode={}, swType={}, swPartNo={}, swVer={}, flashResult={}",
                                vin, ecuSn, vehicleNode, softwareType, softwarePartNo, softwareVersion, flashResult);
                        try {
                            // 调用 SoftwareInventoryAppService 记录软件实装
                            softwareInventoryAppService.applyManifest(
                                    partId,
                                    bindingId,
                                    vin,
                                    softwarePartNo,  // 使用 SOFTWARE_PART_NO 作为 softwareTargetCode
                                    softwarePartNo,
                                    softwareVersion,
                                    null,  // artifactHash
                                    null,  // slot
                                    "UPDATE",  // changeType
                                    "EOL",  // source
                                    batchNum + "_" + vin + "_" + ecuSn + "_" + softwarePartNo,  // sourceEventId
                                    Instant.now(),  // sourceEventTime
                                    Instant.now(),  // reportedAt
                                    true  // isConfirmed (EOL 为 confirmed)
                            );
                        } catch (Exception e) {
                            log.warn("软件实装回写失败: vin={}, ecuSn={}, swPartNo={}, error={}", 
                                    vin, ecuSn, softwarePartNo, e.getMessage());
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理安全回执对账
     * <p>
     * 新格式SECURITY字段映射（按ECU类型区分）：
     * - TBOX: CERT_INJECTED, V2C_COMM_ROOT, TBOX_DEVICE_ROOT
     * - CPT_DCU: CERT_INJECTED, CPT_DCU_DEVICE_ROOT
     * - CGW: CERT_INJECTED, OTA_VEHICLE_ROOT, CGW_DEVICE_ROOT
     * - PEPS: CERT_INJECTED, PEPS_DEVICE_ROOT
     */
    private void processSecurityProvisionConfirm(String vin, JSONArray ecuBaseline) {
        if (ecuBaseline == null || ecuBaseline.isEmpty()) {
            return;
        }

        for (Object ecuObj : ecuBaseline) {
            JSONObject ecu = JSONUtil.parseObj(ecuObj);
            JSONObject security = ecu.getJSONObject("SECURITY");
            
            if (security == null) {
                continue;
            }

            String vehicleNode = ecu.getStr("VEHICLE_NODE");
            String deviceItem = ecu.getStr("DEVICE_ITEM");
            Boolean certInjected = security.getBool("CERT_INJECTED");
            
            log.debug("处理安全回执: vin={}, vehicleNode={}, deviceItem={}, certInjected={}", 
                    vin, vehicleNode, deviceItem, certInjected);

            // 根据VEHICLE_NODE类型处理不同的安全根
            if ("TBOX_5G".equals(vehicleNode)) {
                // TBOX: V2C_COMM_ROOT, TBOX_DEVICE_ROOT
                processSecurityRoot(vin, "V2C_COMM_ROOT", security.getStr("V2C_COMM_ROOT"), "EOL");
                processSecurityRoot(vin, "TBOX_DEVICE_ROOT", security.getStr("TBOX_DEVICE_ROOT"), "EOL");
            } else if ("CPT_DCU_8295".equals(vehicleNode)) {
                // CPT_DCU: CPT_DCU_DEVICE_ROOT
                processSecurityRoot(vin, "CPT_DCU_DEVICE_ROOT", security.getStr("CPT_DCU_DEVICE_ROOT"), "EOL");
            } else if ("CGW_S32G".equals(vehicleNode)) {
                // CGW: OTA_VEHICLE_ROOT, CGW_DEVICE_ROOT
                processSecurityRoot(vin, "OTA_VEHICLE_ROOT", security.getStr("OTA_VEHICLE_ROOT"), "EOL");
                processSecurityRoot(vin, "CGW_DEVICE_ROOT", security.getStr("CGW_DEVICE_ROOT"), "EOL");
            } else if ("PEPS_COMBO".equals(vehicleNode)) {
                // PEPS: PEPS_DEVICE_ROOT
                processSecurityRoot(vin, "PEPS_DEVICE_ROOT", security.getStr("PEPS_DEVICE_ROOT"), "EOL");
            } else {
                // 通用处理：尝试提取常见字段
                processSecurityRoot(vin, "COMM_ROOT", security.getStr("COMM_ROOT"), "EOL");
                processSecurityRoot(vin, "IMMO_ROOT", security.getStr("IMMO_ROOT"), "EOL");
                processSecurityRoot(vin, "OTA_ROOT", security.getStr("OTA_ROOT"), "EOL");
            }
        }
    }

    /**
     * 处理单个安全根的灌注确认
     */
    private void processSecurityRoot(String vin, String rootType, String status, String source) {
        if (StrUtil.isNotBlank(status)) {
            securityProvisionConfirmService.processVehicleSecurityProvision(vin, rootType, status, source);
        }
    }

    /**
     * 处理POWER_DOWN节点
     * <p>
     * 支持两种格式：
     * 1. 时间戳（毫秒）- 新格式
     * 2. 日期字符串（yyyyMMdd）- 旧格式兼容
     */
    private void processPowerDownNode(String vin, JSONObject itemJson) {
        // 新格式：时间戳（毫秒）
        Long powerDownTimeTs = itemJson.getLong("POWER_DOWN_TIME");
        if (powerDownTimeTs != null && powerDownTimeTs > 0) {
            Instant powerDownTime = Instant.ofEpochMilli(powerDownTimeTs);
            vehicleLifecycleAppService.recordPowerDownNode(vin, powerDownTime);
            log.debug("记录POWER_DOWN节点（时间戳）: vin={}, time={}", vin, powerDownTime);
            return;
        }
        
        // 旧格式兼容：日期字符串（yyyyMMdd）
        String powerDownTimeStr = itemJson.getStr("POWER_DOWN_TIME");
        if (StrUtil.isNotBlank(powerDownTimeStr)) {
            try {
                Instant powerDownTime = DateUtil.parse(powerDownTimeStr, "yyyyMMdd").toInstant();
                vehicleLifecycleAppService.recordPowerDownNode(vin, powerDownTime);
                log.debug("记录POWER_DOWN节点（日期字符串）: vin={}, time={}", vin, powerDownTimeStr);
            } catch (Exception e) {
                log.warn("解析POWER_DOWN_TIME失败: vin={}, time={}, error={}", 
                        vin, powerDownTimeStr, e.getMessage());
            }
        }
    }

    /**
     * 按域转发
     */
    private void processDomainForwarding(String vin, JSONArray inspectionItems, 
                                         JSONObject diagnostic, JSONObject otaBaseline, JSONObject powertrain) {
        // INSPECTION_ITEMS→质量/MES域
        if (inspectionItems != null && !inspectionItems.isEmpty()) {
            log.debug("转发INSPECTION_ITEMS到质量/MES域: vin={}, count={}", vin, inspectionItems.size());
            for (Object item : inspectionItems) {
                JSONObject inspection = JSONUtil.parseObj(item);
                String itemCode = inspection.getStr("ITEM_CODE");
                String name = inspection.getStr("NAME");
                String result = inspection.getStr("RESULT");
                String value = inspection.getStr("VALUE");
                String unit = inspection.getStr("UNIT");
                log.debug("检测项: vin={}, code={}, name={}, result={}, value={}, unit={}", 
                        vin, itemCode, name, result, value, unit);
            }
            // TODO: 实际实现需要调用下游服务或发送事件
        }

        // DIAGNOSTIC→诊断服务
        if (diagnostic != null) {
            Boolean dtcCleared = diagnostic.getBool("DTC_CLEARED");
            JSONArray residualDtc = diagnostic.getJSONArray("RESIDUAL_DTC");
            log.debug("转发DIAGNOSTIC到诊断服务: vin={}, dtcCleared={}, residualDtcCount={}", 
                    vin, dtcCleared, residualDtc != null ? residualDtc.size() : 0);
            // TODO: 实际实现需要调用下游服务或发送事件
        }

        // OTA_BASELINE→OTA域
        if (otaBaseline != null) {
            String vehicleVersion = otaBaseline.getStr("VEHICLE_VERSION");
            String packageId = otaBaseline.getStr("PACKAGE_ID");
            String eeArch = otaBaseline.getStr("EE_ARCH");
            log.debug("转发OTA_BASELINE到OTA域: vin={}, vehicleVersion={}, packageId={}, eeArch={}", 
                    vin, vehicleVersion, packageId, eeArch);
            // TODO: 实际实现需要调用下游服务或发送事件
        }

        // POWERTRAIN→动力总成域
        if (powertrain != null) {
            String batteryPackNo = powertrain.getStr("POWER_BATTERY_PACK_NO");
            Integer batterySoh = powertrain.getInt("POWER_BATTERY_SOH");
            String frontMotorNo = powertrain.getStr("FRONT_DRIVE_MOTOR_NO");
            String rearMotorNo = powertrain.getStr("REAR_DRIVE_MOTOR_NO");
            String generatorNo = powertrain.getStr("GENERATOR_NO");
            String engineNo = powertrain.getStr("ENGINE_NO");
            log.debug("转发POWERTRAIN到动力总成域: vin={}, batteryPackNo={}, batterySoh={}, frontMotorNo={}, rearMotorNo={}, generatorNo={}, engineNo={}",
                    vin, batteryPackNo, batterySoh, frontMotorNo, rearMotorNo, generatorNo, engineNo);
            // TODO: 实际实现需要调用下游服务或发送事件
        }
    }
}
