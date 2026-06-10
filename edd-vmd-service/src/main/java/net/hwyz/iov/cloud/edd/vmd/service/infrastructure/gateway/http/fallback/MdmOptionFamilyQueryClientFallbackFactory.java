package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmOptionFamilyQueryClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MDM 选项族查询服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class MdmOptionFamilyQueryClientFallbackFactory implements FallbackFactory<MdmOptionFamilyQueryClient> {

    @Override
    public MdmOptionFamilyQueryClient create(Throwable throwable) {
        return new MdmOptionFamilyQueryClient() {
            @Override
            public List<Map<String, Object>> getAllOptionFamilies() {
                log.error("MDM选项族查询服务调用失败", throwable);
                throw new RuntimeException("MDM选项族查询服务调用失败", throwable);
            }
        };
    }
}
