package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.edd.vmd.api.fallback.MdmOptionFamilyQueryClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * MDM 选项族查询服务接口
 * 调用 MDM 选项族全量快照接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "mdmOptionFamilyQueryClient", value = "edd-mdm", path = "/api/mdm/optionFamily/v1", fallbackFactory = MdmOptionFamilyQueryClientFallbackFactory.class)
public interface MdmOptionFamilyQueryClient {

    /**
     * 获取所有选项族数据
     *
     * @return 选项族数据列表
     */
    @GetMapping("/listAll")
    List<Map<String, Object>> getAllOptionFamilies();

}
