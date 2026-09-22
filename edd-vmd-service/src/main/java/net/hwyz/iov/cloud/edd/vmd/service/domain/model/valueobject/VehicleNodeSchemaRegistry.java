package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 车辆节点模式注册表（迁移期兼容兜底组件，CR-049）
 * <p>
 * 管理车辆节点的配置模式（HSM UID 字段名、预置开关、BizType）。
 * CR-049 后预置资格以 MDM VehicleNode.hsmCapability 主数据为权威来源，
 * 本注册表仅作为 hsmCapability 缺失时的兼容兜底（RD-049-1 / RD-049-6）：
 * 存量节点在 MDM 数据回填完成、缺失指标持续为零后可逐步退场删除。
 * 不再承担新增节点变体的主配置职责（新增变体仅需主数据配置即可生效）。
 * </p>
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
                .bizType(BizType.PEPS_DEVICE_ROOT)
                .description("蓝牙模块，带安全芯片，需要预置ROOT安全常量")
                .build());

        // CCU / CCU_GEN1: 中央计算单元，需要安全常量预置
        register(VehicleNodeSchema.builder()
                .vehicleNodeCode("CCU")
                .hsmUid("HSM")
                .needsSecurityConstantPreset(true)
                .bizType(BizType.CCU_DEVICE_ROOT)
                .description("中央计算单元，带安全芯片，需要预置ROOT安全常量")
                .build());

        register(VehicleNodeSchema.builder()
                .vehicleNodeCode("CCU_GEN1")
                .hsmUid("HSM")
                .needsSecurityConstantPreset(true)
                .bizType(BizType.CCU_DEVICE_ROOT)
                .description("中央计算单元（GEN1），带安全芯片，需要预置ROOT安全常量")
                .build());

        // CGW: 中央网关，可独立或归属CCU，需要安全常量预置
        register(VehicleNodeSchema.builder()
                .vehicleNodeCode("CGW")
                .hsmUid("HSM")
                .needsSecurityConstantPreset(true)
                .bizType(BizType.CGW_DEVICE_ROOT)
                .description("中央网关，带安全芯片，需要预置ROOT安全常量")
                .build());

        // DCU_COCKPIT / DCU_COCKPIT_SA8295P: 座舱域控，需要安全常量预置
        register(VehicleNodeSchema.builder()
                .vehicleNodeCode("DCU_COCKPIT")
                .hsmUid("HSM")
                .needsSecurityConstantPreset(true)
                .bizType(BizType.CPT_DCU_DEVICE_ROOT)
                .description("座舱域控，带安全芯片，需要预置ROOT安全常量")
                .build());

        register(VehicleNodeSchema.builder()
                .vehicleNodeCode("DCU_COCKPIT_SA8295P")
                .hsmUid("HSM")
                .needsSecurityConstantPreset(true)
                .bizType(BizType.CPT_DCU_DEVICE_ROOT)
                .description("座舱域控（SA8295P），带安全芯片，需要预置ROOT安全常量")
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
