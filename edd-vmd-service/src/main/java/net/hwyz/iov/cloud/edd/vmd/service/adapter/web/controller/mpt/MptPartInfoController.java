package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.PartInfoRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.PartInfoResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptPartInfoAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartInfoDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.PartInfoQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInfoAppService;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 物理零件实例相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/partInfo/v1")
public class MptPartInfoController extends BaseController {

    private final PartInfoAppService partInfoAppService;

    /**
     * 分页查询物理零件实例
     *
     * @param partInfo 物理零件实例
     * @return 物理零件实例列表
     */
    @RequiresPermissions("completeVehicle:vehicle:partInfo:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<PartInfoResponse>> list(PartInfoRequest partInfo) {
        log.info("管理后台用户[{}]分页查询物理零件实例", SecurityContextHolder.getUserName());
        startPage();
        PartInfoQuery query = PartInfoQuery.builder()
                .partCode(partInfo.getPartCode())
                .sn(partInfo.getSn())
                .vehicleNodeCode(partInfo.getVehicleNodeCode())
                .instanceState(partInfo.getInstanceState())
                .beginTime(getBeginTime(partInfo))
                .endTime(getEndTime(partInfo))
                .build();
        List<PartInfoDto> dtoList = partInfoAppService.search(query);
        return ApiResponse.ok(getPageResult(PageUtil.convert(dtoList, MptPartInfoAssembler.INSTANCE::fromDto)));
    }

    /**
     * 导出物理零件实例
     *
     * @param response 响应
     * @param partInfo 物理零件实例
     */
    @Log(title = "物理零件实例管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:vehicle:partInfo:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, PartInfoRequest partInfo) {
        log.info("管理后台用户[{}]导出物理零件实例", SecurityContextHolder.getUserName());
    }

    /**
     * 根据物理零件实例ID获取物理零件实例
     *
     * @param partInfoId 物理零件实例ID
     * @return 物理零件实例
     */
    @RequiresPermissions("completeVehicle:vehicle:partInfo:query")
    @GetMapping(value = "/{partInfoId}")
    public ApiResponse<PartInfoResponse> getInfo(@PathVariable Long partInfoId) {
        log.info("管理后台用户[{}]根据物理零件实例ID[{}]获取物理零件实例", SecurityContextHolder.getUserName(), partInfoId);
        return ApiResponse.ok(MptPartInfoAssembler.INSTANCE.fromDto(partInfoAppService.getPartInfoById(partInfoId)));
    }

    /**
     * 新增物理零件实例
     *
     * @param partInfo 物理零件实例
     * @return 结果
     */
    @Log(title = "物理零件实例管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:vehicle:partInfo:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody PartInfoRequest partInfo) {
        log.info("管理后台用户[{}]新增物理零件实例[{}:{}]", SecurityContextHolder.getUserName(), partInfo.getPartCode(), partInfo.getSn());
        if (!partInfoAppService.checkPartCodeAndSnUnique(partInfo.getId(), partInfo.getPartCode(), partInfo.getSn())) {
            return ApiResponse.fail("新增物理零件实例'" + partInfo.getPartCode() + "'失败，物理零件实例已存在");
        }
        partInfoAppService.createPartInfo(MptPartInfoAssembler.INSTANCE.toCmd(partInfo), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 修改保存物理零件实例
     *
     * @param partInfo 物理零件实例
     * @return 结果
     */
    @Log(title = "物理零件实例管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:vehicle:partInfo:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody PartInfoRequest partInfo) {
        log.info("管理后台用户[{}]修改保存物理零件实例[{}:{}]", SecurityContextHolder.getUserName(), partInfo.getPartCode(), partInfo.getSn());
        if (!partInfoAppService.checkPartCodeAndSnUnique(partInfo.getId(), partInfo.getPartCode(), partInfo.getSn())) {
            return ApiResponse.fail("修改保存物理零件实例'" + partInfo.getPartCode() + "'失败，物理零件实例已存在");
        }
        partInfoAppService.modifyPartInfo(MptPartInfoAssembler.INSTANCE.toCmd(partInfo), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 删除物理零件实例
     *
     * @param partInfoIds 物理零件实例ID数组
     * @return 结果
     */
    @Log(title = "物理零件实例管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:vehicle:partInfo:remove")
    @DeleteMapping("/{partInfoIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] partInfoIds) {
        log.info("管理后台用户[{}]删除物理零件实例[{}]", SecurityContextHolder.getUserName(), partInfoIds);
        return partInfoAppService.deletePartInfoByIds(partInfoIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

}
