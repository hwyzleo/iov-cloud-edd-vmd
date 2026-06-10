package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.fallback.MdmOptionCodeQueryClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * MDM 选项值查询服务接口
 * 调用 MDM 选项值全量快照接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "mdmOptionCodeQueryClient", value = "edd-mdm", path = "/api/mdm/optionCode/v1", fallbackFactory = MdmOptionCodeQueryClientFallbackFactory.class)
public interface MdmOptionCodeQueryClient {

    /**
     * 获取所有选项值数据
     *
     * @return 选项值数据列表
     */
    @GetMapping("/listAll")
    List<Map<String, Object>> getAllOptionCodes();

}
