package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ModelRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ModelResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptModelAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ModelDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.ModelQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ModelAppService;
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
 * 车型相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/model/v1")
public class MptModelController extends BaseController {

    private final ModelAppService modelAppService;

    /**
     * 分页查询车型信息
     *
     * @param model 车型信息
     * @return 车型信息列表
     */
    @RequiresPermissions("completeVehicle:product:model:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<ModelResponse>> list(ModelRequest model) {
        log.info("管理后台用户[{}]分页查询车型信息", SecurityUtils.getUsername());
        startPage();
        ModelQuery query = ModelQuery.builder()
                .platformCode(model.getPlatformCode())
                .seriesCode(model.getSeriesCode())
                .code(model.getCode())
                .name(model.getName())
                .beginTime(getBeginTime(model))
                .endTime(getEndTime(model))
                .build();
        List<ModelDto> modelDtoList = modelAppService.search(query);
        return ApiResponse.ok(getPageResult(MptModelAssembler.INSTANCE.fromDtoList(modelDtoList)));
    }

    /**
     * 获取指定车辆平台及车系下的所有车型
     *
     * @param platformCode 车辆平台代码
     * @param seriesCode   车系代码
     * @return 车型信息列表
     */
    @RequiresPermissions("completeVehicle:product:model:list")
    @GetMapping(value = "/listByPlatformCodeAndSeriesCode")
    public ApiResponse<List<ModelResponse>> listByPlatformCodeAndSeriesCode(@RequestParam String platformCode,
                                                                      @RequestParam String seriesCode) {
        log.info("管理后台用户[{}]获取指定车辆平台[{}]及车系[{}]下的所有车型", SecurityUtils.getUsername(),
                platformCode, seriesCode);
        ModelQuery query = ModelQuery.builder()
                .platformCode(platformCode)
                .seriesCode(seriesCode)
                .build();
        List<ModelDto> modelDtoList = modelAppService.search(query);
        return ApiResponse.ok(MptModelAssembler.INSTANCE.fromDtoList(modelDtoList));
    }

    /**
     * 导出车型信息
     *
     * @param response 响应
     * @param model    车型信息
     */
    @Log(title = "车型管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:product:model:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, ModelRequest model) {
        log.info("管理后台用户[{}]导出车型信息", SecurityUtils.getUsername());
    }

    /**
     * 根据车型ID获取车型信息
     *
     * @param modelId 车型ID
     * @return 车型信息
     */
    @RequiresPermissions("completeVehicle:product:model:query")
    @GetMapping(value = "/{modelId}")
    public ApiResponse<ModelResponse> getInfo(@PathVariable Long modelId) {
        log.info("管理后台用户[{}]根据车型ID[{}]获取车型信息", SecurityUtils.getUsername(), modelId);
        return ApiResponse.ok(MptModelAssembler.INSTANCE.fromDto(modelAppService.getModelById(modelId)));
    }

    /**
     * 新增车型信息
     *
     * @param model 车型信息
     * @return 结果
     */
    @Log(title = "车型管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:model:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody ModelRequest model) {
        log.info("管理后台用户[{}]新增车型信息[{}]", SecurityUtils.getUsername(), model.getCode());
        if (!modelAppService.checkCodeUnique(model.getId(), model.getCode())) {
            return ApiResponse.fail("新增车型'" + model.getCode() + "'失败，车型代码已存在");
        }
        modelAppService.createModel(MptModelAssembler.INSTANCE.toCmd(model), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 修改保存车型信息
     *
     * @param model 车型信息
     * @return 结果
     */
    @Log(title = "车型管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:model:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody ModelRequest model) {
        log.info("管理后台用户[{}]修改保存车型信息[{}]", SecurityUtils.getUsername(), model.getCode());
        if (!modelAppService.checkCodeUnique(model.getId(), model.getCode())) {
            return ApiResponse.fail("修改保存车型'" + model.getCode() + "'失败，车型代码已存在");
        }
        modelAppService.modifyModel(MptModelAssembler.INSTANCE.toCmd(model), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 删除车型信息
     *
     * @param modelIds 车型ID数组
     * @return 结果
     */
    @Log(title = "车型管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:product:model:remove")
    @DeleteMapping("/{modelIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] modelIds) {
        log.info("管理后台用户[{}]删除车型信息[{}]", SecurityUtils.getUsername(), modelIds);
        for (Long modelId : modelIds) {
            if (modelAppService.checkModelBaseModelExist(modelId)) {
                return ApiResponse.fail("删除车型'" + modelId + "'失败，该车型下存在基础车型");
            }
            if (modelAppService.checkModelVehicleExist(modelId)) {
                return ApiResponse.fail("删除车型'" + modelId + "'失败，该车型下存在车辆");
            }
        }
        return modelAppService.deleteModelByIds(modelIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

}
