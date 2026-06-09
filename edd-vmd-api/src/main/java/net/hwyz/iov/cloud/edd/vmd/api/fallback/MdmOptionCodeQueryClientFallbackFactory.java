package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.service.MdmOptionCodeQueryClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MDM 选项值查询服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class MdmOptionCodeQueryClientFallbackFactory implements FallbackFactory<MdmOptionCodeQueryClient> {

    @Override
    public MdmOptionCodeQueryClient create(Throwable throwable) {
        return new MdmOptionCodeQueryClient() {
            @Override
            public List<Map<String, Object>> getAllOptionCodes() {
                log.error("MDM选项值查询服务调用失败", throwable);
                throw new RuntimeException("MDM选项值查询服务调用失败", throwable);
            }
        };
    }
}
