package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehicleNodeExResponse;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdVehicleNodeService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 车载节点相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class VmdVehicleNodeServiceFallbackFactory implements FallbackFactory<VmdVehicleNodeService> {

    @Override
    public VmdVehicleNodeService create(Throwable throwable) {
        return new VmdVehicleNodeService() {
            @Override
            public VehicleNodeExResponse getByCode(String code) {
                log.error("车载节点服务根据车载节点代码[{}]查询车载节点信息调用失败", code, throwable);
                return null;
            }

            @Override
            public List<VehicleNodeExResponse> listAllFota() {
                log.error("车载节点服务获取所有FOTA升级车载节点信息调用失败", throwable);
                return null;
            }
        };
    }
}
