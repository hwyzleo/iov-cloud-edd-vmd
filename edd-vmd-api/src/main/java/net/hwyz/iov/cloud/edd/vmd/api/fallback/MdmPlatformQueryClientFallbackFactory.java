package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.service.MdmPlatformQueryClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MDM 平台查询服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class MdmPlatformQueryClientFallbackFactory implements FallbackFactory<MdmPlatformQueryClient> {

    @Override
    public MdmPlatformQueryClient create(Throwable throwable) {
        return new MdmPlatformQueryClient() {
            @Override
            public List<Map<String, Object>> getAllPlatforms() {
                log.error("MDM平台查询服务调用失败", throwable);
                throw new RuntimeException("MDM平台查询服务调用失败", throwable);
            }
        };
    }
}
