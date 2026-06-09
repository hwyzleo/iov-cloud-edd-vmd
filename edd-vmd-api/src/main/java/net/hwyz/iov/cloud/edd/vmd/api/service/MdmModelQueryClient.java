package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.edd.vmd.api.fallback.MdmModelQueryClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * MDM 车型查询服务接口
 * 调用 MDM 车型全量快照接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "mdmModelQueryClient", value = "edd-mdm", path = "/api/mdm/model/v1", fallbackFactory = MdmModelQueryClientFallbackFactory.class)
public interface MdmModelQueryClient {

    /**
     * 获取所有车型数据
     *
     * @return 车型数据列表
     */
    @GetMapping("/listAll")
    List<Map<String, Object>> getAllModels();

}
