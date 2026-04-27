package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptSeriesAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.SeriesRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.SeriesResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.SeriesQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.SeriesDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.SeriesAppService;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
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
@RequestMapping(value = "/api/mpt/series/v1")
public class MptSeriesController extends BaseController {

    private final SeriesAppService seriesAppService;

    /**
     * 分页查询车系信息
     *
     * @param series 车系信息
     * @return 车系信息列表
     */
//    @RequiresPermissions("completeVehicle:product:series:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<SeriesResponse>> list(SeriesRequest series) {
        log.info("管理后台用户[{}]分页查询车系信息", SecurityUtils.getUsername());
        startPage();
        SeriesQuery query = SeriesQuery.builder()
                .platformCode(series.getPlatformCode())
                .code(series.getCode())
                .name(series.getName())
                .beginTime(getBeginTime(series))
                .endTime(getEndTime(series))
                .build();
        List<SeriesDto> seriesDtoList = seriesAppService.search(query);
        return ApiResponse.ok(getPageResult(PageUtil.convert(seriesDtoList, MptSeriesAssembler.INSTANCE::fromDto)));
    }

    /**
     * 获取指定车辆平台下的所有车系
     *
     * @param platformCode 车辆平台代码
     * @return 车系信息列表
     */
    @RequiresPermissions("completeVehicle:product:series:list")
    @GetMapping(value = "/listByPlatformCode")
    public ApiResponse<List<SeriesResponse>> listByPlatformCode(@RequestParam String platformCode) {
        log.info("管理后台用户[{}]获取指定车辆平台[{}]下的所有车系", SecurityUtils.getUsername(), platformCode);
        SeriesQuery query = SeriesQuery.builder()
                .platformCode(platformCode)
                .build();
        List<SeriesDto> seriesDtoList = seriesAppService.search(query);
        return ApiResponse.ok(MptSeriesAssembler.INSTANCE.fromDtoList(seriesDtoList));
    }

    /**
     * 导出车系信息
     *
     * @param response 响应
     * @param series   车系信息
     */
    @Log(title = "车系管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:product:series:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SeriesRequest series) {
        log.info("管理后台用户[{}]导出车系信息", SecurityUtils.getUsername());
    }

    /**
     * 根据车系ID获取车系信息
     *
     * @param seriesId 车系ID
     * @return 车系信息
     */
    @RequiresPermissions("completeVehicle:product:series:query")
    @GetMapping(value = "/{seriesId}")
    public ApiResponse<SeriesResponse> getInfo(@PathVariable Long seriesId) {
        log.info("管理后台用户[{}]根据车系ID[{}]获取车系信息", SecurityUtils.getUsername(), seriesId);
        return ApiResponse.ok(MptSeriesAssembler.INSTANCE.fromDto(seriesAppService.getSeriesById(seriesId)));
    }

    /**
     * 新增车系信息
     *
     * @param series 车系信息
     * @return 结果
     */
    @Log(title = "车系管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:series:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody SeriesRequest series) {
        log.info("管理后台用户[{}]新增车系信息[{}]", SecurityUtils.getUsername(), series.getCode());
        if (!seriesAppService.checkCodeUnique(series.getId(), series.getCode())) {
            return ApiResponse.fail("新增车系'" + series.getCode() + "'失败，车系代码已存在");
        }
        seriesAppService.createSeries(MptSeriesAssembler.INSTANCE.toCmd(series), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 修改保存车系信息
     *
     * @param series 车系信息
     * @return 结果
     */
    @Log(title = "车系管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:series:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody SeriesRequest series) {
        log.info("管理后台用户[{}]修改保存车系信息[{}]", SecurityUtils.getUsername(), series.getCode());
        if (!seriesAppService.checkCodeUnique(series.getId(), series.getCode())) {
            return ApiResponse.fail("修改保存车系'" + series.getCode() + "'失败，车系代码已存在");
        }
        seriesAppService.modifySeries(MptSeriesAssembler.INSTANCE.toCmd(series), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 删除车系信息
     *
     * @param seriesIds 车系ID数组
     * @return 结果
     */
    @Log(title = "车系管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:product:series:remove")
    @DeleteMapping("/{seriesIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] seriesIds) {
        log.info("管理后台用户[{}]删除车系信息[{}]", SecurityUtils.getUsername(), seriesIds);
        for (Long seriesId : seriesIds) {
            if (seriesAppService.checkSeriesModelExist(seriesId)) {
                return ApiResponse.fail("删除车系'" + seriesId + "'失败，该车系下存在车型");
            }
            if (seriesAppService.checkSeriesVehicleExist(seriesId)) {
                return ApiResponse.fail("删除车系'" + seriesId + "'失败，该车系下存在车辆");
            }
        }
        return seriesAppService.deleteSeriesByIds(seriesIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

}
