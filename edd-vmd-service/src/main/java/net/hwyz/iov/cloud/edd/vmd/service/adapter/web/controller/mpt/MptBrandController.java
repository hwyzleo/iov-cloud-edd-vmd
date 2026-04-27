package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.BrandVo;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptBrandAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.BrandDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.BrandQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BrandAppService;
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
 * 车辆品牌相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/brand/v1")
public class MptBrandController extends BaseController {

    private final BrandAppService brandAppService;

    /**
     * 分页查询车辆品牌信息
     *
     * @param brand 车辆品牌信息
     * @return 车辆品牌信息列表
     */
    @RequiresPermissions("completeVehicle:product:brand:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<BrandVo>> list(BrandVo brand) {
        log.info("管理后台用户[{}]分页查询车辆品牌信息", SecurityUtils.getUsername());
        startPage();
        BrandQuery query = BrandQuery.builder()
                .code(brand.getCode())
                .name(brand.getName())
                .beginTime(getBeginTime(brand))
                .endTime(getEndTime(brand))
                .build();
        List<BrandDto> brandDtoList = brandAppService.search(query);
        return ApiResponse.ok(getPageResult(MptBrandAssembler.INSTANCE.fromDtoList(brandDtoList)));
    }

    /**
     * 导出车辆品牌信息
     *
     * @param response 响应
     * @param brand    车辆品牌信息
     */
    @Log(title = "车辆品牌管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:product:brand:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, BrandVo brand) {
        log.info("管理后台用户[{}]导出车辆品牌信息", SecurityUtils.getUsername());
    }

    /**
     * 根据车辆品牌ID获取车辆品牌信息
     *
     * @param brandId 车辆品牌ID
     * @return 车辆品牌信息
     */
    @RequiresPermissions("completeVehicle:product:brand:query")
    @GetMapping(value = "/{brandId}")
    public ApiResponse<BrandVo> getInfo(@PathVariable Long brandId) {
        log.info("管理后台用户[{}]根据车辆品牌ID[{}]获取车辆品牌信息", SecurityUtils.getUsername(), brandId);
        return ApiResponse.ok(MptBrandAssembler.INSTANCE.fromDto(brandAppService.getBrandById(brandId)));
    }

    /**
     * 新增车辆品牌信息
     *
     * @param brand 车辆品牌信息
     * @return 结果
     */
    @Log(title = "车辆品牌管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:brand:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody BrandVo brand) {
        log.info("管理后台用户[{}]新增车辆品牌信息[{}]", SecurityUtils.getUsername(), brand.getCode());
        if (!brandAppService.checkCodeUnique(brand.getId(), brand.getCode())) {
            return ApiResponse.fail("新增车辆品牌'" + brand.getCode() + "'失败，车辆品牌代码已存在");
        }
        brandAppService.createBrand(MptBrandAssembler.INSTANCE.toDto(brand), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 修改保存车辆品牌信息
     *
     * @param brand 车辆品牌信息
     * @return 结果
     */
    @Log(title = "车辆品牌管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:brand:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody BrandVo brand) {
        log.info("管理后台用户[{}]修改保存车辆品牌信息[{}]", SecurityUtils.getUsername(), brand.getCode());
        if (!brandAppService.checkCodeUnique(brand.getId(), brand.getCode())) {
            return ApiResponse.fail("修改保存车辆品牌'" + brand.getCode() + "'失败，车辆品牌代码已存在");
        }
        brandAppService.modifyBrand(MptBrandAssembler.INSTANCE.toDto(brand), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 删除车辆品牌信息
     *
     * @param brandIds 车辆品牌ID数组
     * @return 结果
     */
    @Log(title = "车辆品牌管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:product:brand:remove")
    @DeleteMapping("/{brandIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] brandIds) {
        log.info("管理后台用户[{}]删除车辆品牌信息[{}]", SecurityUtils.getUsername(), brandIds);
        for (Long brandId : brandIds) {
            if (brandAppService.checkBrandSeriesExist(brandId)) {
                return ApiResponse.fail("删除车辆品牌'" + brandId + "'失败，该车辆品牌下存在车系");
            }
            if (brandAppService.checkBrandVehicleExist(brandId)) {
                return ApiResponse.fail("删除车辆品牌'" + brandId + "'失败，该车辆品牌下存在车辆");
            }
        }
        return brandAppService.deleteBrandByIds(brandIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

}
