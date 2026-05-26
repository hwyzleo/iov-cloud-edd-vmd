package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.edd.vmd.api.fallback.MdmPlatformQueryClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * MDM 平台查询服务接口
 * 调用 MDM 平台全量快照接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "mdmPlatformQueryClient", value = "edd-mdm", path = "/api/mdm/platform/v1", fallbackFactory = MdmPlatformQueryClientFallbackFactory.class)
public interface MdmPlatformQueryClient {

    /**
     * 获取所有平台数据
     *
     * @return 平台数据列表
     */
    @GetMapping("/listAll")
    List<Map<String, Object>> getAllPlatforms();

}
