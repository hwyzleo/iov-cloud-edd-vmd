package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.service.MdmModelQueryClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MDM 车型查询服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class MdmModelQueryClientFallbackFactory implements FallbackFactory<MdmModelQueryClient> {

    @Override
    public MdmModelQueryClient create(Throwable throwable) {
        return new MdmModelQueryClient() {
            @Override
            public List<Map<String, Object>> getAllModels() {
                log.error("MDM车型查询服务调用失败", throwable);
                throw new RuntimeException("MDM车型查询服务调用失败", throwable);
            }
        };
    }
}
