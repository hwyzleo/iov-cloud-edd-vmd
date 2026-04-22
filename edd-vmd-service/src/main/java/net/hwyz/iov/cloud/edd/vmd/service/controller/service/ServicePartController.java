package net.hwyz.iov.cloud.edd.vmd.service.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.PartExService;
import net.hwyz.iov.cloud.edd.vmd.service.application.PartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 零件相关服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/part/v1")
public class ServicePartController {

    private final PartAppService partAppService;

    /**
     * 根据零件号查询零件信息
     *
     * @param pn 零件号
     * @return 零件信息
     */
    @GetMapping("/{pn}")
    public PartExService getByPn(@PathVariable String pn) {
        log.info("根据零件号[{}]查询零件信息", pn);
        return PartExServiceMapper.INSTANCE.fromDo(partAppService.getPartByPn(pn));
    }

    /**
     * 获取所有FOTA升级零件信息
     *
     * @return 零件信息
     */
    @GetMapping("/listAllFota")
    public List<PartExService> listAllFota(@RequestParam(required = false) Boolean software) {
        log.info("获取所有FOTA升级零件信息");
        return PartExServiceMapper.INSTANCE.fromDoList(partAppService.listAllFota(software));
    }

}
