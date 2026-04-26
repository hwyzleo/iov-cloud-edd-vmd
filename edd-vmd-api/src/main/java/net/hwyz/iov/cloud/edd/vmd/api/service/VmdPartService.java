package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.edd.vmd.api.vo.PartExService;
import net.hwyz.iov.cloud.edd.vmd.api.fallback.VmdPartServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 零件相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "exPartService", value = ServiceNameConstants.EDD_VMD, path = "/api/service/part/v1", fallbackFactory = VmdPartServiceFallbackFactory.class)
public interface VmdPartService {

    /**
     * 根据零件号查询零件信息
     *
     * @param pn 零件号
     * @return 零件信息
     */
    @GetMapping("/{pn}")
    PartExService getByPn(@PathVariable String pn);

    /**
     * 获取所有FOTA升级零件信息
     *
     * @param software 是否是软件零件
     * @return 零件信息
     */
    @GetMapping("/listAllFota")
    List<PartExService> listAllFota(@RequestParam(required = false) Boolean software);

}
