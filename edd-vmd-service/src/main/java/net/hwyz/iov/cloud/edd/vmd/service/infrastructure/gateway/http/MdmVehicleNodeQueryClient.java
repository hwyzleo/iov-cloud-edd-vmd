package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.fallback.MdmVehicleNodeQueryClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * MDM 车载节点查询服务接口
 * 调用 MDM EEAD 子域车载节点全量快照接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "mdmVehicleNodeQueryClient", value = "edd-mdm", path = "/api/mdm/vehicleNode/v1", fallbackFactory = MdmVehicleNodeQueryClientFallbackFactory.class)
public interface MdmVehicleNodeQueryClient {

    /**
     * 获取所有车载节点数据
     *
     * @return 车载节点数据列表
     */
    @GetMapping("/listAll")
    List<Map<String, Object>> getAllVehicleNodes();

}
