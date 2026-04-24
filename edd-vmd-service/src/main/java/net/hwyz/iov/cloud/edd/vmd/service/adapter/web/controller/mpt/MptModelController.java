package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.ModelVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ModelAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.ModelAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehModelPo;
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
    public ApiResponse<PageResult<ModelVo>> list(ModelVo model) {
        log.info("管理后台用户[{}]分页查询车型信息", SecurityUtils.getUsername());
        startPage();
        List<ModelVo> modelVoList = modelAppService.search(model.getPlatformCode(), model.getSeriesCode(), model.getCode(),
                model.getName(), getBeginTime(model), getEndTime(model));
        return ApiResponse.ok(getPageResult(modelVoList));
    }

    /**
     * 获取指定车辆平台及车系下的所有车型
     *
     * @return 车型信息列表
     */
    @RequiresPermissions("completeVehicle:product:model:list")
    @GetMapping(value = "/listByPlatformCodeAndSeriesCode")
    public ApiResponse<List<ModelVo>> listByPlatformCodeAndSeriesCode(@RequestParam String platformCode, @RequestParam String seriesCode) {
        log.info("管理后台用户[{}]获取指定车辆平台[{}]及车系[{}]下的所有车型", SecurityUtils.getUsername(), platformCode, seriesCode);
        List<ModelVo> modelVoList = modelAppService.search(platformCode, seriesCode, null, null, null, null);
        return ApiResponse.ok(modelVoList);
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
    public void export(HttpServletResponse response, ModelVo model) {
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
    public ApiResponse<ModelVo> getInfo(@PathVariable Long modelId) {
        log.info("管理后台用户[{}]根据车型ID[{}]获取车型信息", SecurityUtils.getUsername(), modelId);
        VehModelPo modelPo = modelAppService.getModelById(modelId);
        return ApiResponse.ok(ModelAssembler.INSTANCE.fromPo(modelPo));
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
    public ApiResponse<Void> add(@Validated @RequestBody ModelVo model) {
        log.info("管理后台用户[{}]新增车型信息[{}]", SecurityUtils.getUsername(), model.getCode());
        if (!modelAppService.checkCodeUnique(model.getId(), model.getCode())) {
            return ApiResponse.fail("新增车型'" + model.getCode() + "'失败，车型代码已存在");
        }
        VehModelPo modelPo = ModelAssembler.INSTANCE.toPo(model);
        modelPo.setCreateBy(SecurityUtils.getUserId().toString());
        return modelAppService.createModel(modelPo) > 0 ? ApiResponse.ok() : ApiResponse.fail("新增失败");
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
    public ApiResponse<Void> edit(@Validated @RequestBody ModelVo model) {
        log.info("管理后台用户[{}]修改保存车型信息[{}]", SecurityUtils.getUsername(), model.getCode());
        if (!modelAppService.checkCodeUnique(model.getId(), model.getCode())) {
            return ApiResponse.fail("修改保存车型'" + model.getCode() + "'失败，车型代码已存在");
        }
        VehModelPo modelPo = ModelAssembler.INSTANCE.toPo(model);
        modelPo.setModifyBy(SecurityUtils.getUserId().toString());
        return modelAppService.modifyModel(modelPo) > 0 ? ApiResponse.ok() : ApiResponse.fail("修改失败");
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
            if (modelAppService.checkModelBasicModelExist(modelId)) {
                return ApiResponse.fail("删除车型'" + modelId + "'失败，该车型下存在基础车型");
            }
            if (modelAppService.checkModelModelConfigExist(modelId)) {
                return ApiResponse.fail("删除车型'" + modelId + "'失败，该车型下存在车型配置");
            }
            if (modelAppService.checkModelVehicleExist(modelId)) {
                return ApiResponse.fail("删除车型'" + modelId + "'失败，该车型下存在车辆");
            }
        }
        return modelAppService.deleteModelByIds(modelIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

}
