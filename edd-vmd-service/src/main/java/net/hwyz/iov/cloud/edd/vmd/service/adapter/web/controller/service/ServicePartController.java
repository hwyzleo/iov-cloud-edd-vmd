package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.PartExService;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.PartExServiceAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartAppService;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 零件对外服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/part/v1")
public class ServicePartController extends BaseController {

    private final PartAppService partAppService;

    /**
     * 根据零件号查询零件信息
     *
     * @param pn 零件号
     * @return 零件信息
     */
    @GetMapping("/{pn}")
    public ApiResponse<PartExService> getByPn(@PathVariable String pn) {
        log.info("内部服务请求根据零件号[{}]查询零件信息", pn);
        return ApiResponse.ok(PartExServiceAssembler.INSTANCE.fromDomain(partAppService.getPartByPn(pn)));
    }

    /**
     * 获取所有FOTA升级零件信息
     *
     * @param software 是否是软件零件
     * @return 零件信息
     */
    @GetMapping("/listAllFota")
    public ApiResponse<List<PartExService>> listAllFota(@RequestParam(required = false) Boolean software) {
        log.info("内部服务请求获取所有FOTA升级零件信息，是否软件零件：[{}]", software);
        return ApiResponse.ok(PartExServiceAssembler.INSTANCE.fromDomainList(partAppService.listAllFota(software)));
    }

}
