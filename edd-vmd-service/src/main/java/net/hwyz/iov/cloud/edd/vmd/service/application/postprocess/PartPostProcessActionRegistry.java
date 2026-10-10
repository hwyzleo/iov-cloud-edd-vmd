package net.hwyz.iov.cloud.edd.vmd.service.application.postprocess;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 零件导入后置处理动作注册表
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 * <p>
 * 按稳定动作类型注册并发现 {@link PartPostProcessActionHandler}；
 * 新增后置能力仅需新增 Handler 实现并自注册，无需修改主编排（D34）。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
@Component
public class PartPostProcessActionRegistry {

    private final ConcurrentHashMap<String, PartPostProcessActionHandler> registry = new ConcurrentHashMap<>();

    @Autowired
    public PartPostProcessActionRegistry(List<PartPostProcessActionHandler> handlers) {
        for (PartPostProcessActionHandler handler : handlers) {
            register(handler);
        }
    }

    /**
     * 注册动作处理器
     *
     * @param handler 动作处理器
     */
    public void register(PartPostProcessActionHandler handler) {
        String actionType = handler.actionType();
        PartPostProcessActionHandler existing = registry.putIfAbsent(actionType, handler);
        if (existing != null) {
            log.warn("动作类型[{}]的处理器已存在，忽略重复注册", actionType);
        } else {
            log.info("注册零件导入后置处理动作[{}]的处理器[{}]", actionType, handler.getClass().getSimpleName());
        }
    }

    /**
     * 获取动作处理器
     *
     * @param actionType 动作类型
     * @return 处理器，未注册返回 null
     */
    public PartPostProcessActionHandler getHandler(String actionType) {
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
}
