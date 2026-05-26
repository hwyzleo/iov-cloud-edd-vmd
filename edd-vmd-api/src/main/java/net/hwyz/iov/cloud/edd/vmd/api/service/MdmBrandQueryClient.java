package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.edd.vmd.api.fallback.MdmBrandQueryClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * MDM 品牌查询服务接口
 * 调用 MDM 品牌全量快照接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "mdmBrandQueryClient", value = "edd-mdm", path = "/api/mdm/brand/v1", fallbackFactory = MdmBrandQueryClientFallbackFactory.class)
public interface MdmBrandQueryClient {

    /**
     * 获取所有品牌数据
     *
     * @return 品牌数据列表
     */
    @GetMapping("/listAll")
    List<Map<String, Object>> getAllBrands();

}
