package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptBaseModelAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.BaseModelFeatureCodeRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.BaseModelRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.BaseModelFeatureCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.BaseModelResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.BaseModelQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BaseModelDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BaseModelFeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BaseModelAppService;
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
 * 基础车型相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/baseModel/v1")
public class MptBaseModelController extends BaseController {

    private final BaseModelAppService baseModelAppService;

    /**
     * 分页查询基础车型信息
     *
     * @param baseModel 基础车型信息
     * @return 基础车型信息列表
     */
    @RequiresPermissions("completeVehicle:product:baseModel:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<BaseModelResponse>> list(BaseModelRequest baseModel) {
        log.info("管理后台用户[{}]分页查询基础车型信息", SecurityUtils.getUsername());
        startPage();
        BaseModelQuery query = BaseModelQuery.builder()
                .platformCode(baseModel.getPlatformCode())
                .seriesCode(baseModel.getSeriesCode())
                .modelCode(baseModel.getModelCode())
                .code(baseModel.getCode())
                .name(baseModel.getName())
                .beginTime(getBeginTime(baseModel))
                .endTime(getEndTime(baseModel))
                .build();
        List<BaseModelDto> baseModelDtoList = baseModelAppService.search(query);
        return ApiResponse.ok(getPageResult(MptBaseModelAssembler.INSTANCE.fromDtoList(baseModelDtoList)));
    }

    /**
     * 查询基础车型下特征值
     *
     * @param baseModelCode        基础车型编码
     * @param baseModelFeatureCode 基础车型特征值
     * @return 基础车型下特征值列表
     */
    @RequiresPermissions("completeVehicle:product:baseModel:list")
    @GetMapping(value = "/{baseModelCode}/featureCode/list")
    public ApiResponse<List<BaseModelFeatureCodeResponse>> listFeatureCode(@PathVariable String baseModelCode, BaseModelFeatureCodeRequest baseModelFeatureCode) {
        log.info("管理后台用户[{}]分页查询基础车型下特征值", SecurityUtils.getUsername());
        List<BaseModelFeatureCodeDto> dtoList = baseModelAppService.searchFeatureCode(baseModelCode, baseModelFeatureCode.getFamilyCode());
        return ApiResponse.ok(MptBaseModelAssembler.INSTANCE.fromFeatureCodeDtoList(dtoList));
    }

    /**
     * 获取指定车辆平台及车系及车型下的所有基础车型
     *
     * @param platformCode 车辆平台代码
     * @param seriesCode   车系代码
     * @param modelCode    车型代码
     * @return 基础车型信息列表
     */
    @RequiresPermissions("completeVehicle:product:baseModel:list")
    @GetMapping(value = "/listByPlatformCodeAndSeriesCodeAndModelCode")
    public ApiResponse<List<BaseModelResponse>> listByPlatformCodeAndSeriesCodeAndModelCode(@RequestParam String platformCode,
                                                                                             @RequestParam String seriesCode,
                                                                                             @RequestParam String modelCode) {
        log.info("管理后台用户[{}]获取指定车辆平台[{}]及车系[{}]及车型[{}]下的所有基础车型", SecurityUtils.getUsername(),
                platformCode, seriesCode, modelCode);
        BaseModelQuery query = BaseModelQuery.builder()
                .platformCode(platformCode)
                .seriesCode(seriesCode)
                .modelCode(modelCode)
                .build();
        List<BaseModelDto> baseModelDtoList = baseModelAppService.search(query);
        return ApiResponse.ok(MptBaseModelAssembler.INSTANCE.fromDtoList(baseModelDtoList));
    }

    /**
     * 导出基础车型信息
     *
     * @param response  响应
     * @param baseModel 基础车型信息
     */
    @Log(title = "基础车型管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:product:baseModel:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, BaseModelRequest baseModel) {
        log.info("管理后台用户[{}]导出基础车型信息", SecurityUtils.getUsername());
    }

    /**
     * 根据基础车型ID获取基础车型信息
     *
     * @param baseModelId 基础车型ID
     * @return 基础车型信息
     */
    @RequiresPermissions("completeVehicle:product:baseModel:query")
    @GetMapping(value = "/{baseModelId}")
    public ApiResponse<BaseModelResponse> getInfo(@PathVariable Long baseModelId) {
        log.info("管理后台用户[{}]根据基础车型ID[{}]获取基础车型信息", SecurityUtils.getUsername(), baseModelId);
        return ApiResponse.ok(MptBaseModelAssembler.INSTANCE.fromDto(baseModelAppService.getBaseModelById(baseModelId)));
    }

    /**
     * 根据基础车型特征值ID获取基础车型特征值信息
     *
     * @param baseModelCode          基础车型编码
     * @param baseModelFeatureCodeId 基础车型特征值ID
     * @return 基础车型特征值信息
     */
    @RequiresPermissions("completeVehicle:product:baseModel:query")
    @GetMapping(value = "/{baseModelCode}/featureCode/{baseModelFeatureCodeId}")
    public ApiResponse<BaseModelFeatureCodeResponse> getFeatureCodeInfo(@PathVariable String baseModelCode, @PathVariable Long baseModelFeatureCodeId) {
        log.info("管理后台用户[{}]根据基础车型[{}]特征值ID[{}]获取基础车型特征值信息", SecurityUtils.getUsername(), baseModelCode, baseModelFeatureCodeId);
        return ApiResponse.ok(MptBaseModelAssembler.INSTANCE.fromFeatureCodeDto(baseModelAppService.getBaseModelFeatureCodeById(baseModelFeatureCodeId)));
    }

    /**
     * 新增基础车型信息
     *
     * @param baseModel 基础车型信息
     * @return 结果
     */
    @Log(title = "基础车型管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:baseModel:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody BaseModelRequest baseModel) {
        log.info("管理后台用户[{}]新增基础车型信息[{}]", SecurityUtils.getUsername(), baseModel.getCode());
        if (!baseModelAppService.checkCodeUnique(baseModel.getId(), baseModel.getCode())) {
            return ApiResponse.fail("新增基础车型'" + baseModel.getCode() + "'失败，基础车型代码已存在");
        }
        baseModelAppService.createBasicModel(MptBaseModelAssembler.INSTANCE.toCmd(baseModel));
        return ApiResponse.ok();
    }

    /**
     * 新增基础车型特征值
     *
     * @param baseModelCode        基础车型编码
     * @param baseModelFeatureCode 基础车型特征值
     * @return 结果
     */
    @Log(title = "基础车型管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:baseModel:edit")
    @PostMapping("/{baseModelCode}/featureCode")
    public ApiResponse<Void> addFeatureCode(@PathVariable String baseModelCode, @Validated @RequestBody BaseModelFeatureCodeRequest baseModelFeatureCode) {
        log.info("管理后台用户[{}]新增基础车型[{}]特征值[{}]", SecurityUtils.getUsername(), baseModelCode, baseModelFeatureCode.getFamilyCode());
        if (!baseModelAppService.checkFeatureCodeUnique(baseModelFeatureCode.getId(), baseModelCode, baseModelFeatureCode.getFamilyCode())) {
            return ApiResponse.fail("新增基础车型特征值'" + baseModelFeatureCode.getFamilyCode() + "'失败，基础车型特征值已存在");
        }
        baseModelAppService.createBasicModelFeatureCode(MptBaseModelAssembler.INSTANCE.toFeatureCodeCmd(baseModelFeatureCode));
        return ApiResponse.ok();
    }

    /**
     * 修改保存基础车型信息
     *
     * @param baseModel 基础车型信息
     * @return 结果
     */
    @Log(title = "基础车型管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:baseModel:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody BaseModelRequest baseModel) {
        log.info("管理后台用户[{}]修改保存基础车型信息[{}]", SecurityUtils.getUsername(), baseModel.getCode());
        if (!baseModelAppService.checkCodeUnique(baseModel.getId(), baseModel.getCode())) {
            return ApiResponse.fail("修改保存基础车型'" + baseModel.getCode() + "'失败，基础车型代码已存在");
        }
        baseModelAppService.modifyBasicModel(MptBaseModelAssembler.INSTANCE.toCmd(baseModel));
        return ApiResponse.ok();
    }

    /**
     * 修改保存基础车型特征值
     *
     * @param baseModelCode        基础车型编码
     * @param baseModelFeatureCode 基础车型特征值
     * @return 结果
     */
    @Log(title = "基础车型管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:baseModel:edit")
    @PutMapping("/{baseModelCode}/featureCode")
    public ApiResponse<Void> editFeatureCode(@PathVariable String baseModelCode, @Validated @RequestBody BaseModelFeatureCodeRequest baseModelFeatureCode) {
        log.info("管理后台用户[{}]修改保存基础车型[{}]特征值[{}]", SecurityUtils.getUsername(), baseModelCode, baseModelFeatureCode.getFamilyCode());
        if (!baseModelAppService.checkFeatureCodeUnique(baseModelFeatureCode.getId(), baseModelCode, baseModelFeatureCode.getFamilyCode())) {
            return ApiResponse.fail("修改保存基础车型特征值'" + baseModelFeatureCode.getFamilyCode() + "'失败，基础车型特征值已存在");
        }
        baseModelAppService.modifyBaseModelFeatureCode(MptBaseModelAssembler.INSTANCE.toFeatureCodeCmd(baseModelFeatureCode));
        return ApiResponse.ok();
    }

    /**
     * 删除基础车型信息
     *
     * @param baseModelIds 基础车型ID数组
     * @return 结果
     */
    @Log(title = "基础车型管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:product:baseModel:remove")
    @DeleteMapping("/{baseModelIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] baseModelIds) {
        log.info("管理后台用户[{}]删除基础车型信息[{}]", SecurityUtils.getUsername(), baseModelIds);
        for (Long baseModelId : baseModelIds) {
            if (baseModelAppService.checkBaseModelBuildConfigExist(baseModelId)) {
                return ApiResponse.fail("删除基础车型'" + baseModelId + "'失败，该基础车型下存在生产配置");
            }
            if (baseModelAppService.checkBaseModelVehicleExist(baseModelId)) {
                return ApiResponse.fail("删除基础车型'" + baseModelId + "'失败，该基础车型下存在车辆");
            }
        }
        return baseModelAppService.deleteBasicModelByIds(baseModelIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

    /**
     * 删除基础车型特征值
     *
     * @param baseModelCode           基础车型编码
     * @param baseModelFeatureCodeIds 基础车型特征值ID数组
     * @return 结果
     */
    @Log(title = "基础车型管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:baseModel:edit")
    @DeleteMapping("/{baseModelCode}/featureCode/{baseModelFeatureCodeIds}")
    public ApiResponse<Void> removeFeatureCode(@PathVariable String baseModelCode, @PathVariable Long[] baseModelFeatureCodeIds) {
        log.info("管理后台用户[{}]删除基础车型[{}]特征值[{}]", SecurityUtils.getUsername(), baseModelCode, baseModelFeatureCodeIds);
        return baseModelAppService.deleteBaseModelFeatureCodeByIds(baseModelFeatureCodeIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }
}
