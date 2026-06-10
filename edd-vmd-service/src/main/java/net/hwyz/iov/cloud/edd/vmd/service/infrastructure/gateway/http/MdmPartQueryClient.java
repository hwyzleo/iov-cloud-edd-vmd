package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.fallback.MdmPartQueryClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * MDM 零件查询服务接口
 * 调用 MDM Part 子域零件全量快照接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "mdmPartQueryClient", value = "edd-mdm", path = "/api/mdm/part/v1", fallbackFactory = MdmPartQueryClientFallbackFactory.class)
public interface MdmPartQueryClient {

    /**
     * 获取所有零件数据
     *
     * @return 零件数据列表
     */
    @GetMapping("/listAll")
    List<Map<String, Object>> getAllParts();

}
