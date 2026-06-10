package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmCarLineQueryClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MDM 车系查询服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class MdmCarLineQueryClientFallbackFactory implements FallbackFactory<MdmCarLineQueryClient> {

    @Override
    public MdmCarLineQueryClient create(Throwable throwable) {
        return new MdmCarLineQueryClient() {
            @Override
            public List<Map<String, Object>> getAllSeries() {
                log.error("MDM车系查询服务调用失败", throwable);
                throw new RuntimeException("MDM车系查询服务调用失败", throwable);
            }
        };
    }
}
