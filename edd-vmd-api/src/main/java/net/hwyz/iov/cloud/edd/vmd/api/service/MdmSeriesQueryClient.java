package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.edd.vmd.api.fallback.MdmSeriesQueryClientFallbackFactory;
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
@FeignClient(contextId = "mdmSeriesQueryClient", value = "edd-mdm", path = "/api/mdm/series/v1", fallbackFactory = MdmSeriesQueryClientFallbackFactory.class)
public interface MdmSeriesQueryClient {

    /**
     * 获取所有车系数据
     *
     * @return 车系数据列表
     */
    @GetMapping("/listAll")
    List<Map<String, Object>> getAllSeries();

}
