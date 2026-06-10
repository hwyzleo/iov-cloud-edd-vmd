package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmVehicleNodeQueryClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MDM 车载节点查询服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class MdmVehicleNodeQueryClientFallbackFactory implements FallbackFactory<MdmVehicleNodeQueryClient> {

    @Override
    public MdmVehicleNodeQueryClient create(Throwable throwable) {
        return new MdmVehicleNodeQueryClient() {
            @Override
            public List<Map<String, Object>> getAllVehicleNodes() {
                log.error("MDM车载节点查询服务调用失败", throwable);
                throw new RuntimeException("MDM车载节点查询服务调用失败", throwable);
            }
        };
    }
}
