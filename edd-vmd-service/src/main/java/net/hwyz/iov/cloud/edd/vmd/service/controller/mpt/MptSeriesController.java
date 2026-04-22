package net.hwyz.iov.cloud.edd.vmd.service.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.SeriesVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.SeriesAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.SeriesMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehSeriesDo;
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
    @RequiresPermissions("completeVehicle:product:series:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<SeriesVo>> list(SeriesVo series) {
        log.info("管理后台用户[{}]分页查询车系信息", SecurityUtils.getUsername());
        startPage();
        List<SeriesVo> seriesVoList = seriesAppService.search(series.getPlatformCode(), series.getCode(),
                series.getName(), getBeginTime(series), getEndTime(series));
        return ApiResponse.ok(getPageResult(seriesVoList));
    }

    /**
     * 获取指定车辆平台下的所有车系
     *
     * @param platformCode 车辆平台代码
     * @return 车系列表
     */
    @RequiresPermissions("completeVehicle:product:series:list")
    @GetMapping(value = "/listByPlatformCode")
    public ApiResponse<List<SeriesVo>> listByPlatformCode(@RequestParam String platformCode) {
        log.info("管理后台用户[{}]获取指定车辆平台[{}]下的所有车系", SecurityUtils.getUsername(), platformCode);
        List<SeriesVo> seriesVoList = seriesAppService.search(platformCode, null, null, null, null);
        return ApiResponse.ok(seriesVoList);
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
    public void export(HttpServletResponse response, SeriesVo series) {
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
    public ApiResponse<SeriesVo> getInfo(@PathVariable Long seriesId) {
        log.info("管理后台用户[{}]根据车系ID[{}]获取车系信息", SecurityUtils.getUsername(), seriesId);
        VmdVehSeriesDo seriesPo = seriesAppService.getSeriesById(seriesId);
        return ApiResponse.ok(SeriesMapper.INSTANCE.fromDo(seriesPo));
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
    public ApiResponse<Void> add(@Validated @RequestBody SeriesVo series) {
        log.info("管理后台用户[{}]新增车系信息[{}]", SecurityUtils.getUsername(), series.getCode());
        if (!seriesAppService.checkCodeUnique(series.getId(), series.getCode())) {
            return ApiResponse.fail("新增车系'" + series.getCode() + "'失败，车系代码已存在");
        }
        VmdVehSeriesDo seriesPo = SeriesMapper.INSTANCE.toDo(series);
        seriesPo.setCreateBy(SecurityUtils.getUserId().toString());
        return seriesAppService.createSeries(seriesPo) > 0 ? ApiResponse.ok() : ApiResponse.fail("新增失败");
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
    public ApiResponse<Void> edit(@Validated @RequestBody SeriesVo series) {
        log.info("管理后台用户[{}]修改保存车系信息[{}]", SecurityUtils.getUsername(), series.getCode());
        if (!seriesAppService.checkCodeUnique(series.getId(), series.getCode())) {
            return ApiResponse.fail("修改保存车系'" + series.getCode() + "'失败，车系代码已存在");
        }
        VmdVehSeriesDo seriesPo = SeriesMapper.INSTANCE.toDo(series);
        seriesPo.setModifyBy(SecurityUtils.getUserId().toString());
        return seriesAppService.modifySeries(seriesPo) > 0 ? ApiResponse.ok() : ApiResponse.fail("修改失败");
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
            if (seriesAppService.checkSeriesBasicModelExist(seriesId)) {
                return ApiResponse.fail("删除车系'" + seriesId + "'失败，该车系下存在基础车型");
            }
            if (seriesAppService.checkSeriesModelConfigExist(seriesId)) {
                return ApiResponse.fail("删除车系'" + seriesId + "'失败，该车系下存在车型配置");
            }
            if (seriesAppService.checkSeriesVehicleExist(seriesId)) {
                return ApiResponse.fail("删除车系'" + seriesId + "'失败，该车系下存在车辆");
            }
        }
        return seriesAppService.deleteSeriesByIds(seriesIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

}
