package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptCarLineAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.CarLineRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.CarLineResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.CarLineQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CarLineDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.CarLineAppService;
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
 * 车系相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/carLine/v1")
public class MptCarLineController extends BaseController {

    private final CarLineAppService carLineAppService;

    /**
     * 分页查询车系信息
     *
     * @param carLine 车系信息
     * @return 车系信息列表
     */
    @RequiresPermissions("completeVehicle:product:carLine:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<CarLineResponse>> list(CarLineRequest carLine) {
        log.info("管理后台用户[{}]分页查询车系信息", SecurityContextHolder.getUserName());
        startPage();
        CarLineQuery query = CarLineQuery.builder()
                .brandCode(carLine.getBrandCode())
                .code(carLine.getCode())
                .name(carLine.getName())
                .beginTime(getBeginTime(carLine))
                .endTime(getEndTime(carLine))
                .build();
        List<CarLineDto> carLineDtoList = carLineAppService.search(query);
        return ApiResponse.ok(getPageResult(PageUtil.convert(carLineDtoList, MptCarLineAssembler.INSTANCE::fromDto)));
    }

    /**
     * 获取指定品牌下的所有车系
     *
     * @param brandCode 品牌代码
     * @return 车系信息列表
     */
    @RequiresPermissions("completeVehicle:product:carLine:list")
    @GetMapping(value = "/listByBrandCode")
    public ApiResponse<List<CarLineResponse>> listByBrandCode(@RequestParam(required = false) String brandCode) {
        log.info("管理后台用户[{}]获取指定品牌[{}]下的所有车系", SecurityContextHolder.getUserName(), brandCode);
        CarLineQuery query = CarLineQuery.builder()
                .brandCode(brandCode)
                .build();
        List<CarLineDto> carLineDtoList = carLineAppService.search(query);
        return ApiResponse.ok(MptCarLineAssembler.INSTANCE.fromDtoList(carLineDtoList));
    }

    /**
     * 导出车系信息
     *
     * @param response 响应
     * @param carLine  车系信息
     */
    @Log(title = "车系管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:product:carLine:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, CarLineRequest carLine) {
        log.info("管理后台用户[{}]导出车系信息", SecurityContextHolder.getUserName());
    }

    /**
     * 根据车系ID获取车系信息
     *
     * @param carLineId 车系ID
     * @return 车系信息
     */
    @RequiresPermissions("completeVehicle:product:carLine:query")
    @GetMapping(value = "/{carLineId}")
    public ApiResponse<CarLineResponse> getInfo(@PathVariable Long carLineId) {
        log.info("管理后台用户[{}]根据车系ID[{}]获取车系信息", SecurityContextHolder.getUserName(), carLineId);
        return ApiResponse.ok(MptCarLineAssembler.INSTANCE.fromDto(carLineAppService.getSeriesById(carLineId)));
    }

    /**
     * 新增车系信息
     *
     * @param carLine 车系信息
     * @return 结果
     */
    @Log(title = "车系管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:carLine:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody CarLineRequest carLine) {
        log.info("管理后台用户[{}]新增车系信息[{}]", SecurityContextHolder.getUserName(), carLine.getCode());
        if (!carLineAppService.checkCodeUnique(carLine.getId(), carLine.getCode())) {
            return ApiResponse.fail("新增车系'" + carLine.getCode() + "'失败，车系代码已存在");
        }
        carLineAppService.createSeries(MptCarLineAssembler.INSTANCE.toCmd(carLine), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 修改保存车系信息
     *
     * @param carLine 车系信息
     * @return 结果
     */
    @Log(title = "车系管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:carLine:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody CarLineRequest carLine) {
        log.info("管理后台用户[{}]修改保存车系信息[{}]", SecurityContextHolder.getUserName(), carLine.getCode());
        if (!carLineAppService.checkCodeUnique(carLine.getId(), carLine.getCode())) {
            return ApiResponse.fail("修改保存车系'" + carLine.getCode() + "'失败，车系代码已存在");
        }
        carLineAppService.modifySeries(MptCarLineAssembler.INSTANCE.toCmd(carLine), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 删除车系信息
     *
     * @param carLineIds 车系ID数组
     * @return 结果
     */
    @Log(title = "车系管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:product:carLine:remove")
    @DeleteMapping("/{carLineIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] carLineIds) {
        log.info("管理后台用户[{}]删除车系信息[{}]", SecurityContextHolder.getUserName(), carLineIds);
        for (Long carLineId : carLineIds) {
            if (carLineAppService.checkSeriesModelExist(carLineId)) {
                return ApiResponse.fail("删除车系'" + carLineId + "'失败，该车系下存在车型");
            }
            if (carLineAppService.checkSeriesVehicleExist(carLineId)) {
                return ApiResponse.fail("删除车系'" + carLineId + "'失败，该车系下存在车辆");
            }
        }
        return carLineAppService.deleteSeriesByIds(carLineIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

}
