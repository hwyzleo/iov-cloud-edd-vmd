package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.edd.vmd.api.fallback.MdmCarLineQueryClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * MDM 车系查询服务接口
 * 调用 MDM 车系全量快照接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "mdmCarLineQueryClient", value = "edd-mdm", path = "/api/mdm/carLine/v1", fallbackFactory = MdmCarLineQueryClientFallbackFactory.class)
public interface MdmCarLineQueryClient {

    /**
     * 获取所有车系数据
     *
     * @return 车系数据列表
     */
    @GetMapping("/listAll")
    List<Map<String, Object>> getAllSeries();

}
