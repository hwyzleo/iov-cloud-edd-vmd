package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.ManufacturerVo;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptManufacturerAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.ManufacturerDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.ManufacturerQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ManufacturerAppService;
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
 * 生产厂商相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/manufacturer/v1")
public class MptManufacturerController extends BaseController {

    private final ManufacturerAppService manufacturerAppService;

    /**
     * 分页查询生产厂商信息
     *
     * @param manufacturer 生产厂商信息
     * @return 生产厂商信息列表
     */
    @RequiresPermissions("completeVehicle:product:manufacturer:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<ManufacturerVo>> list(ManufacturerVo manufacturer) {
        log.info("管理后台用户[{}]分页查询生产厂商信息", SecurityUtils.getUsername());
        startPage();
        ManufacturerQuery query = ManufacturerQuery.builder()
                .code(manufacturer.getCode())
                .name(manufacturer.getName())
                .beginTime(getBeginTime(manufacturer))
                .endTime(getEndTime(manufacturer))
                .build();
        List<ManufacturerDto> manufacturerDtoList = manufacturerAppService.search(query);
        return ApiResponse.ok(getPageResult(MptManufacturerAssembler.INSTANCE.fromDtoList(manufacturerDtoList)));
    }

    /**
     * 导出生产厂商信息
     *
     * @param response     响应
     * @param manufacturer 生产厂商信息
     */
    @Log(title = "生产厂商管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:product:manufacturer:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, ManufacturerVo manufacturer) {
        log.info("管理后台用户[{}]导出生产厂商信息", SecurityUtils.getUsername());
    }

    /**
     * 根据生产厂商ID获取生产厂商信息
     *
     * @param manufacturerId 生产厂商ID
     * @return 生产厂商信息
     */
    @RequiresPermissions("completeVehicle:product:manufacturer:query")
    @GetMapping(value = "/{manufacturerId}")
    public ApiResponse<ManufacturerVo> getInfo(@PathVariable Long manufacturerId) {
        log.info("管理后台用户[{}]根据生产厂商ID[{}]获取生产厂商信息", SecurityUtils.getUsername(), manufacturerId);
        return ApiResponse.ok(MptManufacturerAssembler.INSTANCE.fromDto(manufacturerAppService.getManufacturerById(manufacturerId)));
    }

    /**
     * 新增生产厂商信息
     *
     * @param manufacturer 生产厂商信息
     * @return 结果
     */
    @Log(title = "生产厂商管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:manufacturer:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody ManufacturerVo manufacturer) {
        log.info("管理后台用户[{}]新增生产厂商信息[{}]", SecurityUtils.getUsername(), manufacturer.getCode());
        if (!manufacturerAppService.checkCodeUnique(manufacturer.getId(), manufacturer.getCode())) {
            return ApiResponse.fail("新增生产厂商'" + manufacturer.getCode() + "'失败，生产厂商代码已存在");
        }
        manufacturerAppService.createManufacturer(MptManufacturerAssembler.INSTANCE.toDto(manufacturer), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 修改保存生产厂商信息
     *
     * @param manufacturer 生产厂商信息
     * @return 结果
     */
    @Log(title = "生产厂商管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:manufacturer:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody ManufacturerVo manufacturer) {
        log.info("管理后台用户[{}]修改保存生产厂商信息[{}]", SecurityUtils.getUsername(), manufacturer.getCode());
        if (!manufacturerAppService.checkCodeUnique(manufacturer.getId(), manufacturer.getCode())) {
            return ApiResponse.fail("修改保存生产厂商'" + manufacturer.getCode() + "'失败，生产厂商代码已存在");
        }
        manufacturerAppService.modifyManufacturer(MptManufacturerAssembler.INSTANCE.toDto(manufacturer), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 删除生产厂商信息
     *
     * @param manufacturerIds 生产厂商ID数组
     * @return 结果
     */
    @Log(title = "生产厂商管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:product:manufacturer:remove")
    @DeleteMapping("/{manufacturerIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] manufacturerIds) {
        log.info("管理后台用户[{}]删除生产厂商信息[{}]", SecurityUtils.getUsername(), manufacturerIds);
        for (Long manufacturerId : manufacturerIds) {
            if (manufacturerAppService.checkManufacturerVehicleExist(manufacturerId)) {
                return ApiResponse.fail("删除生产厂商'" + manufacturerId + "'失败，该生产厂商下存在车辆");
            }
        }
        return manufacturerAppService.deleteManufacturerByIds(manufacturerIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

}
