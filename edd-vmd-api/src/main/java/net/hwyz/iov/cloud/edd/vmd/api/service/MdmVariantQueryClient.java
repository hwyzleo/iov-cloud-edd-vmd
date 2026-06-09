package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.edd.vmd.api.fallback.MdmVariantQueryClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * MDM 版本查询服务接口
 * 调用 MDM 版本全量快照接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "mdmVariantQueryClient", value = "edd-mdm", path = "/api/mdm/variant/v1", fallbackFactory = MdmVariantQueryClientFallbackFactory.class)
public interface MdmVariantQueryClient {

    /**
     * 获取所有版本数据
     *
     * @return 版本数据列表
     */
    @GetMapping("/listAll")
    List<Map<String, Object>> getAllVariants();

}
