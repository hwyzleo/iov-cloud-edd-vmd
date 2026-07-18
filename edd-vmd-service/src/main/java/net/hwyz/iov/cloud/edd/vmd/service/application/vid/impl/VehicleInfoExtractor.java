package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleDetail;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 车辆下线数据字段映射提取器
 * <p>
 * 从 EOL JSON 数据中提取车辆基础信息、详情字段、EOL 日期等。
 * 将原先散落在解析器中的 30+ 字段映射逻辑集中管理。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class VehicleInfoExtractor extends BaseProcessor {

    /**
     * 车辆基础信息字段映射定义
     */
    private static final List<FieldMapping> BASIC_INFO_MAPPINGS = List.of(
            new FieldMapping("MANUFACTURER", "plantCode", "工厂数据"),
            new FieldMapping("BRAND", "brandCode", "品牌数据"),
            new FieldMapping("PLATFORM", "platformCode", "平台数据"),
            new FieldMapping("SERIES", "carLineCode", "车系数据"),
            new FieldMapping("MODEL", "modelCode", "车型数据"),
            new FieldMapping("BASE_MODEL", "variantCode", "版本数据"),
            new FieldMapping("BUILD_CONFIG", "configurationCode", "生产配置数据"),
            new FieldMapping("VEHICLE_BASE_VERSION", "vehicleBaseVersion", "车辆基线版本")
    );

    /**
     * 车辆详情字段映射定义
     */
    private static final List<FieldMapping> DETAIL_MAPPINGS = List.of(
            new FieldMapping("PRODUCTION_ORDER", null, "生产订单"),
            new FieldMapping("MATNR", null, "整车物料编码"),
            new FieldMapping("PROJECT", null, "车型项目"),
            new FieldMapping("SALES_AREA", null, "销售区域"),
            new FieldMapping("BODY_TYPE", null, "车身形式"),
            new FieldMapping("CONFIG_LEVEL", null, "配置等级"),
            new FieldMapping("MODEL_YEAR", null, "车型年份"),
            new FieldMapping("STEERING_POSITION", null, "左右舵"),
            new FieldMapping("INTERIOR_STYLE", null, "内饰风格"),
            new FieldMapping("EXTERIOR_COLOR", null, "外饰颜色"),
            new FieldMapping("DRIVE_TYPE", null, "驾驶形式"),
            new FieldMapping("WHEEL", null, "轮毂"),
            new FieldMapping("TIRE", null, "轮胎"),
            new FieldMapping("SEAT_TYPE", null, "座椅类型"),
            new FieldMapping("ASSISTED_DRIVING", null, "辅助驾驶"),
            new FieldMapping("ETC_SYSTEM", null, "ETC系统"),
            new FieldMapping("REAR_TOW_BAR", null, "后牵引杆"),
            new FieldMapping("ENGINE_NO", null, "发动机编码"),
            new FieldMapping("ENGINE_TYPE", null, "发动机类型"),
            new FieldMapping("FRONT_DRIVE_MOTOR_NO", null, "前驱电机编码"),
            new FieldMapping("FRONT_DRIVE_MOTOR_TYPE", null, "前驱电机类型"),
            new FieldMapping("REAR_DRIVE_MOTOR_NO", null, "后驱电机编码"),
            new FieldMapping("REAR_DRIVE_MOTOR_TYPE", null, "后驱电机类型"),
            new FieldMapping("GENERATOR_NO", null, "发电机编码"),
            new FieldMapping("GENERATOR_TYPE", null, "发电机类型"),
            new FieldMapping("POWER_BATTERY_PACK_NO", null, "动力电池包编码"),
            new FieldMapping("POWER_BATTERY_TYPE", null, "动力电池类型"),
            new FieldMapping("POWER_BATTERY_FACTORY", null, "动力电池厂商")
    );

    /**
     * 残档车辆默认值（EOL 兜底）
     * 用于填充 NOT NULL 字段，避免数据库约束冲突
     */
    private static final String STUB_DEFAULT = "UNKNOWN";

    /**
     * 创建残档车辆（EOL 兜底）
     * <p>
     * 缺七项生产配置与选项值快照，需后续 PRODUCE 重导补全。
     * 为满足数据库 NOT NULL 约束，设置默认值为 UNKNOWN。
     *
     * @param itemJson 车辆 JSON 数据
     * @param batchNum 批次号
     * @param vin      车架号
     * @return 残档车辆基础信息
     */
    public VehicleBasicInfo createStubVehicle(JSONObject itemJson, String batchNum, String vin) {
        VehicleBasicInfo basicInfo = VehicleBasicInfo.builder()
                .vin(vin)
                .plantCode(STUB_DEFAULT)
                .brandCode(STUB_DEFAULT)
                .platformCode(STUB_DEFAULT)
                .carLineCode(STUB_DEFAULT)
                .modelCode(STUB_DEFAULT)
                .variantCode(STUB_DEFAULT)
                .configurationCode(STUB_DEFAULT)
                .build();
        // 尝试从 JSON 中提取基础版本信息
        handleVehicleInfo(itemJson, basicInfo, "VEHICLE_BASE_VERSION", "vehicleBaseVersion", "车辆基线版本", batchNum, vin);
        return basicInfo;
    }

    /**
     * 提取车辆基础信息
     * <p>
     * EOL数据中可能不包含完整的七项生产配置（MANUFACTURER/BRAND/PLATFORM/SERIES/MODEL/BASE_MODEL/BUILD_CONFIG），
     * 这些字段通常在PRODUCE数据中提供。EOL场景下仅提取存在的字段，缺失的字段不打印WARN日志。
     *
     * @param itemJson 车辆 JSON 数据
     * @param existing 已有的车辆基础信息（可能为 null）
     * @param batchNum 批次号
     * @param vin      车架号
     * @return 填充后的车辆基础信息
     */
    public VehicleBasicInfo extractBasicInfo(JSONObject itemJson, VehicleBasicInfo existing, String batchNum, String vin) {
        VehicleBasicInfo basicInfo = existing;
        if (ObjUtil.isNull(basicInfo)) {
            basicInfo = VehicleBasicInfo.builder().vin(vin).build();
        }
        for (FieldMapping mapping : BASIC_INFO_MAPPINGS) {
            String keyValue = itemJson.getStr(mapping.jsonKey);
            if (StrUtil.isNotBlank(keyValue)) {
                Object fieldValue = cn.hutool.core.bean.BeanUtil.getFieldValue(basicInfo, mapping.propertyName);
                if (ObjUtil.isNull(fieldValue) || StrUtil.isBlank(fieldValue.toString())) {
                    cn.hutool.core.bean.BeanUtil.setFieldValue(basicInfo, mapping.propertyName, keyValue.trim().toUpperCase());
                } else if (!keyValue.trim().equalsIgnoreCase(fieldValue.toString())) {
                    log.warn("车辆导入数据批次号[{}]车辆[{}]{}[{}]与原数据[{}]不一致", batchNum, vin, mapping.desc, keyValue.trim(), fieldValue);
                }
            }
            // EOL场景下不打印字段为空的WARN日志，因为这些字段可能在PRODUCE数据中提供
        }
        return basicInfo;
    }

    /**
     * 提取车辆详情
     * <p>
     * EOL数据中可能不包含完整的详情字段（如生产订单、物料编码、配置信息等），
     * 这些字段通常在PRODUCE数据中提供。EOL场景下仅提取存在的字段，缺失的字段不打印WARN日志。
     *
     * @param itemJson        车辆 JSON 数据
     * @param vehicleDetailMap 已有的详情 Map（key 为 type）
     * @param batchNum        批次号
     * @param vin             车架号
     * @return 更新后的详情列表
     */
    public List<VehicleDetail> extractDetails(JSONObject itemJson, Map<String, VehicleDetail> vehicleDetailMap,
                                              String batchNum, String vin) {
        for (FieldMapping mapping : DETAIL_MAPPINGS) {
            String keyValue = itemJson.getStr(mapping.jsonKey);
            if (StrUtil.isNotBlank(keyValue)) {
                VehicleDetail vehicleDetail = vehicleDetailMap.get(mapping.jsonKey);
                if (vehicleDetail == null) {
                    vehicleDetailMap.put(mapping.jsonKey, VehicleDetail.builder()
                            .vin(vin)
                            .type(mapping.jsonKey)
                            .val(keyValue)
                            .build());
                } else if (!keyValue.trim().equalsIgnoreCase(vehicleDetail.getVal())) {
                    log.warn("车辆导入数据批次号[{}]车辆[{}]{}[{}]与原数据[{}]不一致", batchNum, vin, mapping.desc, keyValue.trim(), vehicleDetail.getVal());
                }
            }
            // EOL场景下不打印字段为空的WARN日志，因为这些字段可能在PRODUCE数据中提供
        }
        return vehicleDetailMap.values().stream().collect(Collectors.toList());
    }

    /**
     * 提取 EOL 日期
     *
     * @param itemJson 车辆 JSON 数据
     * @return EOL 日期，如果字段为空则返回当前时间
     */
    public Instant extractEolDate(JSONObject itemJson) {
        String eolDateStr = itemJson.getStr("EOL_DATE");
        if (StrUtil.isNotBlank(eolDateStr)) {
            return DateUtil.parse(eolDateStr, "yyyyMMdd").toInstant();
        }
        return Instant.now();
    }

    /**
     * 提取合格证日期字符串
     *
     * @param itemJson 车辆 JSON 数据
     * @return 合格证日期字符串，可能为 null
     */
    public String extractCertDateStr(JSONObject itemJson) {
        return itemJson.getStr("CERT_DATE");
    }

    /**
     * 字段映射定义
     */
    private record FieldMapping(String jsonKey, String propertyName, String desc) {
    }
}
