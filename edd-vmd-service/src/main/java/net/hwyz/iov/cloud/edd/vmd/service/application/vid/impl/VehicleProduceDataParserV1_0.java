package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleProduceEventEnvelope;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehiclePublish;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleSecurityPresetAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.VehicleImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleOption;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleOptionRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 整车主档后台导入解析器 (US-040)
 * 处理 type=PRODUCE 的整车主档批量导入
 * <p>
 * VMD-DSN-CR-027: 车辆数据导入域独立化，解析器注册到独立命名空间
 * <p>
 * VMD-DSN-CR-039: 正常生产流程也写入 Outbox，供下游（OTA 等）消费
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
    private final VehicleOptionRepository vehicleOptionRepository;
    private final VmdOutboxRepository vmdOutboxRepository;

    /**
     * Kafka Topic
     */
    private static final String KAFKA_TOPIC = "vmd.vehicle.produce.event";

    /**
     * 事件类型
     */
    private static final String EVENT_TYPE = "VehicleProduceEvent";

    /**
     * 聚合类型
     */
    private static final String AGGREGATE_TYPE = "VEHICLE";

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
                // 提取并保存选项值快照
                try {
                    List<VehicleOption> options = extractVehicleOptions(vin, itemJson, batchNum);
                    if (!options.isEmpty()) {
                        vehicleOptionRepository.batchUpsert(options);
                        log.debug("保存车辆选项值快照: vin={}, count={}", vin, options.size());
                    }
                } catch (Exception e) {
                    log.warn("车辆[{}]选项值快照保存失败: {}", vin, e.getMessage());
                }

                // 发布 Spring 进程内事件（触发生命周期节点记录、安全常量预置等）
                vehiclePublish.produce(vin, batchNum);

                // 写入 Outbox，供 Relay 发布到 Kafka（下游 OTA 等消费）
                try {
                    publishToOutbox(vin, vehicleBasicInfo, batchNum);
                } catch (Exception e) {
                    log.warn("车辆[{}]写入Outbox失败: {}", vin, e.getMessage());
                    // Outbox 写入失败不计入 failureCount，因为它是异步投递步骤
                }

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

    /**
     * 发布车辆生产事件到 Outbox
     * <p>
     * 构造 VehicleProduceEventEnvelope 并写入 vmd_outbox，
     * 由 Outbox Relay 发布到 Kafka。
     *
     * @param vin            车架号
     * @param vehicleBasicInfo 车辆基础信息
     * @param batchNum       批次号
     */
    private void publishToOutbox(String vin, VehicleBasicInfo vehicleBasicInfo, String batchNum) {
        // 构造事件payload
        VehicleProduceEventEnvelope.VehicleProducePayload payload = VehicleProduceEventEnvelope.VehicleProducePayload.builder()
                .vin(vin)
                .produceTime(LocalDateTime.now())
                .plantCode(vehicleBasicInfo.getPlantCode())
                .brandCode(vehicleBasicInfo.getBrandCode())
                .platformCode(vehicleBasicInfo.getPlatformCode())
                .carLineCode(vehicleBasicInfo.getCarLineCode())
                .modelCode(vehicleBasicInfo.getModelCode())
                .variantCode(vehicleBasicInfo.getVariantCode())
                .configurationCode(vehicleBasicInfo.getConfigurationCode())
                .orderNum(vehicleBasicInfo.getOrderNum())
                .build();

        // 构造事件信封
        VehicleProduceEventEnvelope envelope = VehicleProduceEventEnvelope.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EVENT_TYPE)
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(vin)
                .version(System.currentTimeMillis())
                .occurredAt(LocalDateTime.now())
                .producer("vmd-import")
                .payload(payload)
                .replay(false)
                .batchNum(batchNum)
                .build();

        // 写入Outbox
        VmdOutbox outbox = VmdOutbox.builder()
                .eventId(envelope.getEventId())
                .eventType(EVENT_TYPE)
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(vin)
                .aggregateVersion(envelope.getVersion())
                .topic(KAFKA_TOPIC)
                .messageKey(vin)
                .payload(JSONUtil.toJsonStr(envelope))
                .publishState("PENDING")
                .retryCount(0)
                .sourceType("IMPORT")
                .sourceRefId(batchNum)
                .createTime(LocalDateTime.now())
                .build();
        vmdOutboxRepository.insert(outbox);

        log.debug("车辆[{}]生产事件写入Outbox成功", vin);
    }

    /**
     * 从导入数据中提取选项值快照
     *
     * @param vin       车辆识别码
     * @param dataJson  导入数据 JSON
     * @param batchNum  批次号
     * @return 选项值快照列表
     */
    private List<VehicleOption> extractVehicleOptions(String vin, JSONObject dataJson, String batchNum) {
        List<VehicleOption> options = new ArrayList<>();
        JSONArray optionArray = dataJson.getJSONArray("OPTIONS");
        if (optionArray != null) {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < optionArray.size(); i++) {
                JSONObject optionObj = optionArray.getJSONObject(i);
                String familyCode = optionObj.getStr("OPTION_FAMILY");
                String code = optionObj.getStr("OPTION_CODE");
                if (StrUtil.isNotBlank(familyCode) && StrUtil.isNotBlank(code)) {
                    options.add(VehicleOption.builder()
                            .vin(vin)
                            .optionFamilyCode(familyCode)
                            .optionCode(code)
                            .source("PRODUCE")
                            .batchNum(batchNum)
                            .snapshotTime(now)
                            .build());
                }
            }
        }
        return options;
    }
}
