package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.fallback.MdmPlantQueryClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * MDM 工厂查询服务接口
 * 调用 MDM 工厂全量快照接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "mdmPlantQueryClient", value = "edd-mdm", path = "/api/mdm/plant/v1", fallbackFactory = MdmPlantQueryClientFallbackFactory.class)
public interface MdmPlantQueryClient {

    /**
     * 获取所有工厂数据
     *
     * @return 工厂数据列表
     */
    @GetMapping("/listAll")
    List<Map<String, Object>> getAllPlants();

}
