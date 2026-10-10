package net.hwyz.iov.cloud.edd.vmd.service.application.replay;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleImportReplayActionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 车辆导入补发动作注册表
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发扩展为按 ImportType 路由的动作补偿
 * <p>
 * 按稳定动作类型注册并发现 {@link VehicleImportReplayAction}；
 * 按 ImportType 返回有序动作集合（生命周期补齐 → 绑定事件 → 软件实装事件；
 * PRODUCE 仅既有生产事件补发）。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
@Component
public class VehicleImportReplayActionRegistry {

    private final ConcurrentHashMap<String, VehicleImportReplayAction> registry = new ConcurrentHashMap<>();

    @Autowired
    public VehicleImportReplayActionRegistry(List<VehicleImportReplayAction> actions) {
        for (VehicleImportReplayAction action : actions) {
            register(action);
        }
    }

    /**
     * 注册动作处理器
     *
     * @param action 动作处理器
     */
    public void register(VehicleImportReplayAction action) {
        String actionType = action.actionType();
        VehicleImportReplayAction existing = registry.putIfAbsent(actionType, action);
        if (existing != null) {
            log.warn("动作类型[{}]的处理器已存在，忽略重复注册", actionType);
        } else {
            log.info("注册车辆导入补发动作[{}]的处理器[{}]", actionType, action.getClass().getSimpleName());
        }
    }

    /**
     * 获取动作处理器
     *
     * @param actionType 动作类型
     * @return 处理器，未注册返回 null
     */
    public VehicleImportReplayAction getAction(String actionType) {
        return registry.get(actionType);
    }

    /**
     * 是否已登记该动作类型
     *
     * @param actionType 动作类型
     * @return 是否已登记
     */
    public boolean isRegistered(String actionType) {
        return registry.containsKey(actionType);
    }

    /**
     * 按导入类型返回有序动作集合（仅已注册动作）
     *
     * @param importType 导入类型
     * @return 有序动作集合
     */
    public List<VehicleImportReplayAction> actionsForImportType(String importType) {
        List<VehicleImportReplayAction> result = new ArrayList<>();
        for (VehicleImportReplayActionType type : VehicleImportReplayActionType.values()) {
            if (type.applicableTo(importType)) {
                VehicleImportReplayAction action = registry.get(type.getValue());
                if (action != null) {
                    result.add(action);
                }
            }
        }
        return result;
    }
}
