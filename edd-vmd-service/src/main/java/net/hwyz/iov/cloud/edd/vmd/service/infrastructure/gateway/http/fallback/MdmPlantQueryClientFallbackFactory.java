package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmPlantQueryClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MDM 工厂查询服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class MdmPlantQueryClientFallbackFactory implements FallbackFactory<MdmPlantQueryClient> {

    @Override
    public MdmPlantQueryClient create(Throwable throwable) {
        return new MdmPlantQueryClient() {
            @Override
            public List<Map<String, Object>> getAllPlants() {
                log.error("MDM工厂查询服务调用失败", throwable);
                throw new RuntimeException("MDM工厂查询服务调用失败", throwable);
            }
        };
    }
}
