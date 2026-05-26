package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.service.MdmBrandQueryClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MDM 品牌查询服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class MdmBrandQueryClientFallbackFactory implements FallbackFactory<MdmBrandQueryClient> {

    @Override
    public MdmBrandQueryClient create(Throwable throwable) {
        return new MdmBrandQueryClient() {
            @Override
            public List<Map<String, Object>> getAllBrands() {
                log.error("MDM品牌查询服务调用失败", throwable);
                throw new RuntimeException("MDM品牌查询服务调用失败", throwable);
            }
        };
    }
}
