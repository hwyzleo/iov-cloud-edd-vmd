package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmPartQueryClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MDM 零件查询服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class MdmPartQueryClientFallbackFactory implements FallbackFactory<MdmPartQueryClient> {

    @Override
    public MdmPartQueryClient create(Throwable throwable) {
        return new MdmPartQueryClient() {
            @Override
            public List<Map<String, Object>> getAllParts() {
                log.error("MDM零件查询服务调用失败", throwable);
                throw new RuntimeException("MDM零件查询服务调用失败", throwable);
            }
        };
    }
}
