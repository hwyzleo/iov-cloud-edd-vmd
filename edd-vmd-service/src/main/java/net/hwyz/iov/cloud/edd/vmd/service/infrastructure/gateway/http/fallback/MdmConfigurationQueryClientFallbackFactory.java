package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmConfigurationQueryClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MDM 配置查询服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class MdmConfigurationQueryClientFallbackFactory implements FallbackFactory<MdmConfigurationQueryClient> {

    @Override
    public MdmConfigurationQueryClient create(Throwable throwable) {
        return new MdmConfigurationQueryClient() {
            @Override
            public List<Map<String, Object>> getAllConfigurations() {
                log.error("MDM配置查询服务调用失败", throwable);
                throw new RuntimeException("MDM配置查询服务调用失败", throwable);
            }
        };
    }
}
