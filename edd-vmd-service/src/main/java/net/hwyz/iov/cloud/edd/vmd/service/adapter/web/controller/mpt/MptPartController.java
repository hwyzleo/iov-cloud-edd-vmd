package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.PartVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.PartAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartPo;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 零件信息相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/part/v1")
public class MptPartController extends BaseController {

    private final PartAppService partAppService;

    /**
     * 分页查询零件信息
     *
     * @param part 零件信息
     * @return 零件信息列表
     */
    @RequiresPermissions("completeVehicle:vehicle:part:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<PartVo>> list(PartVo part) {
        log.info("管理后台用户[{}]分页查询零件信息", SecurityUtils.getUsername());
        startPage();
        List<PartVo> partVoList = partAppService.search(part.getKey(), part.getPn(), part.getName(), part.getType(),
                part.getDeviceCode(), getBeginTime(part), getEndTime(part));
        return ApiResponse.ok(getPageResult(partVoList));
    }

    /**
     * 导出零件信息
     *
     * @param response 响应
     * @param part     设备信息
     */
    @Log(title = "零件信息管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:vehicle:part:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, PartVo part) {
        log.info("管理后台用户[{}]导出零件信息", SecurityUtils.getUsername());
    }

    /**
     * 根据零件信息ID获取零件信息
     *
     * @param partId 零件信息ID
     * @return 零件信息信息
     */
    @RequiresPermissions("completeVehicle:vehicle:part:query")
    @GetMapping(value = "/{partId}")
    public ApiResponse getInfo(@PathVariable Long partId) {
        log.info("管理后台用户[{}]根据零件信息ID[{}]获取零件信息", SecurityUtils.getUsername(), partId);
        PartPo partPo = partAppService.getPartById(partId);
        return ApiResponse.ok(PartAssembler.INSTANCE.fromPo(partPo));
    }

    /**
     * 新增零件信息
     *
     * @param part 零件信息
     * @return 结果
     */
    @Log(title = "零件信息管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:vehicle:part:add")
    @PostMapping
    public ApiResponse add(@Validated @RequestBody PartVo part) {
        log.info("管理后台用户[{}]新增零件信息[{}]", SecurityUtils.getUsername(), part.getPn());
        if (!partAppService.checkPnUnique(part.getId(), part.getPn())) {
            return ApiResponse.fail("新增零件信息'" + part.getPn() + "'失败，零件号已存在");
        }
        PartPo partPo = PartAssembler.INSTANCE.toPo(part);
        partPo.setCreateBy(SecurityUtils.getUserId().toString());
        return partAppService.createPart(partPo) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

    /**
     * 修改保存零件信息
     *
     * @param part 零件信息
     * @return 结果
     */
    @Log(title = "零件信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:vehicle:part:edit")
    @PutMapping
    public ApiResponse edit(@Validated @RequestBody PartVo part) {
        log.info("管理后台用户[{}]修改保存零件信息[{}]", SecurityUtils.getUsername(), part.getPn());
        if (!partAppService.checkPnUnique(part.getId(), part.getPn())) {
            return ApiResponse.fail("修改保存零件信息'" + part.getPn() + "'失败，零件号已存在");
        }
        PartPo partPo = PartAssembler.INSTANCE.toPo(part);
        partPo.setModifyBy(SecurityUtils.getUserId().toString());
        return partAppService.modifyPart(partPo) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

    /**
     * 删除零件信息
     *
     * @param partIds 零件信息ID数组
     * @return 结果
     */
    @Log(title = "零件信息管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:vehicle:part:remove")
    @DeleteMapping("/{partIds}")
    public ApiResponse remove(@PathVariable Long[] partIds) {
        log.info("管理后台用户[{}]删除零件信息[{}]", SecurityUtils.getUsername(), partIds);
        return partAppService.deletePartByIds(partIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

}
