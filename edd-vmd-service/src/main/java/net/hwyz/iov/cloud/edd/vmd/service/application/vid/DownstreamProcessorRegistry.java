package net.hwyz.iov.cloud.edd.vmd.service.application.vid;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 下游处理器注册表
 * 管理所有DownstreamProcessor实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class DownstreamProcessorRegistry {

    private final ConcurrentHashMap<String, DownstreamProcessor> registry = new ConcurrentHashMap<>();

    @Autowired
    public DownstreamProcessorRegistry(List<DownstreamProcessor> processors) {
        for (DownstreamProcessor processor : processors) {
            register(processor);
        }
    }

    /**
     * 注册处理器
     *
     * @param processor 处理器
     */
    public void register(DownstreamProcessor processor) {
        String key = processor.getSupportedVehicleNodeCode();
        DownstreamProcessor existing = registry.putIfAbsent(key, processor);
        if (existing != null) {
            log.warn("车载节点代码[{}]的处理器已存在，忽略重复注册", key);
        } else {
            log.info("注册车载节点代码[{}]的处理器[{}]", key, processor.getClass().getSimpleName());
        }
    }

    /**
     * 获取处理器
     *
     * @param vehicleNodeCode 车载节点代码
     * @return 处理器，如果不存在返回null
     */
    public DownstreamProcessor getProcessor(String vehicleNodeCode) {
        return registry.get(vehicleNodeCode);
    }
}
