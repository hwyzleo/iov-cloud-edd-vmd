package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.service.MdmVariantQueryClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MDM 版本查询服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class MdmVariantQueryClientFallbackFactory implements FallbackFactory<MdmVariantQueryClient> {

    @Override
    public MdmVariantQueryClient create(Throwable throwable) {
        return new MdmVariantQueryClient() {
            @Override
            public List<Map<String, Object>> getAllVariants() {
                log.error("MDM版本查询服务调用失败", throwable);
                throw new RuntimeException("MDM版本查询服务调用失败", throwable);
            }
        };
    }
}
