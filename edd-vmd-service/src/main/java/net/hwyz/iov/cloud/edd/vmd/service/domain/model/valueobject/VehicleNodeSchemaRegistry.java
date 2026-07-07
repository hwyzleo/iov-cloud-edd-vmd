package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 车辆节点模式注册表
 * <p>
 * 管理车辆节点的配置模式，包括是否需要安全常量预置等属性
 * 根据 vehicleNodeCode 查询对应的 VehicleNodeSchema
 *
 * @author hwyz_leo
 * @since 2026-06-24
 */
@Slf4j
@Component
public class VehicleNodeSchemaRegistry {

    private final Map<String, VehicleNodeSchema> registry = new ConcurrentHashMap<>();

    public VehicleNodeSchemaRegistry() {
        // 初始化内置车辆节点模式
        registerBuiltinSchemas();
    }

    /**
     * 注册车辆节点模式
     *
     * @param schema 车辆节点模式
     */
    public void register(VehicleNodeSchema schema) {
        String key = schema.getVehicleNodeCode();
        VehicleNodeSchema existing = registry.putIfAbsent(key, schema);
        if (existing != null) {
            log.warn("车辆节点[{}]的模式已存在，忽略重复注册", key);
        } else {
            log.info("注册车辆节点[{}]的模式, needsSecurityConstantPreset={}", key, schema.isNeedsSecurityConstantPreset());
        }
    }

    /**
     * 获取车辆节点模式
     *
     * @param vehicleNodeCode 车辆节点编码
     * @return 车辆节点模式，如果不存在返回null
     */
    public VehicleNodeSchema getSchema(String vehicleNodeCode) {
        if (vehicleNodeCode == null) {
            return null;
        }
        return registry.get(vehicleNodeCode);
    }

    /**
     * 判断车辆节点是否需要安全常量预置
     *
     * @param vehicleNodeCode 车辆节点编码
     * @return true-需要，false-不需要
     */
    public boolean needsSecurityConstantPreset(String vehicleNodeCode) {
        if (vehicleNodeCode == null) {
            return false;
        }
        VehicleNodeSchema schema = registry.get(vehicleNodeCode);
        return schema != null && schema.isNeedsSecurityConstantPreset();
    }

    /**
     * 获取车辆节点的HSM UID字段名
     *
     * @param vehicleNodeCode 车辆节点编码
     * @return HSM UID字段名，如果不存在返回null
     */
    public String getHsmUidField(String vehicleNodeCode) {
        if (vehicleNodeCode == null) {
            return null;
        }
        VehicleNodeSchema schema = registry.get(vehicleNodeCode);
        return schema != null ? schema.getHsmUid() : null;
    }

    /**
     * 获取车辆节点的业务类型枚举
     *
     * @param vehicleNodeCode 车辆节点编码
     * @return 业务类型枚举，如果不存在返回null
     */
    public BizType getBizType(String vehicleNodeCode) {
        if (vehicleNodeCode == null) {
            return null;
        }
        VehicleNodeSchema schema = registry.get(vehicleNodeCode);
        return schema != null ? schema.getBizType() : null;
    }

    /**
     * 注册内置车辆节点模式
     */
    private void registerBuiltinSchemas() {
        // TBOX: 车联终端，需要安全常量预置
        register(VehicleNodeSchema.builder()
                .vehicleNodeCode("TBOX_5G")
                .hsmUid("HSM")
                .needsSecurityConstantPreset(true)
                .bizType(BizType.TBOX_DEVICE_ROOT)
                .description("车联终端，带安全芯片，需要预置ROOT安全常量")
                .build());

        register(VehicleNodeSchema.builder()
                .vehicleNodeCode("TBOX")
                .hsmUid("HSM")
                .needsSecurityConstantPreset(true)
                .bizType(BizType.TBOX_DEVICE_ROOT)
                .description("车联终端，带安全芯片，需要预置ROOT安全常量")
                .build());

        // BTM: 蓝牙模块，需要安全常量预置
        register(VehicleNodeSchema.builder()
                .vehicleNodeCode("BTM")
                .hsmUid("HSM")
                .needsSecurityConstantPreset(true)
                .bizType(BizType.TBOX_DEVICE_ROOT)
                .description("蓝牙模块，带安全芯片，需要预置ROOT安全常量")
                .build());

        // CCP: 中央计算平台，需要安全常量预置
        register(VehicleNodeSchema.builder()
                .vehicleNodeCode("CCP")
                .hsmUid("HSM")
                .needsSecurityConstantPreset(true)
                .bizType(BizType.TBOX_DEVICE_ROOT)
                .description("中央计算平台，带安全芯片，需要预置ROOT安全常量")
                .build());

        // IDCM: 智驾模块，需要安全常量预置
        register(VehicleNodeSchema.builder()
                .vehicleNodeCode("IDCM")
                .hsmUid("HSM")
                .needsSecurityConstantPreset(true)
                .bizType(BizType.TBOX_DEVICE_ROOT)
                .description("智驾模块，带安全芯片，需要预置ROOT安全常量")
                .build());

        // TSP: SIM卡节点，不需要安全常量预置
        register(VehicleNodeSchema.builder()
                .vehicleNodeCode("TSP")
                .needsSecurityConstantPreset(false)
                .description("SIM卡节点，无安全芯片")
                .build());

        log.info("内置车辆节点模式注册完成，共{}种类型", registry.size());
    }
}
