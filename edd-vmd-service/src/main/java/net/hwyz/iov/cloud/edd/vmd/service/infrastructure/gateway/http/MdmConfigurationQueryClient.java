package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.fallback.MdmConfigurationQueryClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * MDM 配置查询服务接口
 * 调用 MDM 配置全量快照接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "mdmConfigurationQueryClient", value = "edd-mdm", path = "/api/mdm/configuration/v1", fallbackFactory = MdmConfigurationQueryClientFallbackFactory.class)
public interface MdmConfigurationQueryClient {

    /**
     * 获取所有配置数据
     *
     * @return 配置数据列表
     */
    @GetMapping("/listAll")
    List<Map<String, Object>> getAllConfigurations();

}
