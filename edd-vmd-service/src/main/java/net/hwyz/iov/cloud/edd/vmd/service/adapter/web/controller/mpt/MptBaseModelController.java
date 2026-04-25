package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.BaseModelFeatureCodeVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.BaseModelVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BaseModelAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.FeatureFamilyAppService;
import net.hwyz.iov.cloud.edd.vmd.api.vo.FeatureCodeVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.FeatureFamilyVo;
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
    private final FeatureFamilyAppService featureFamilyAppService;

    /**
     * 分页查询基础车型信息
     *
     * @param baseModel 基础车型信息
     * @return 基础车型信息列表
     */
    @RequiresPermissions("completeVehicle:product:baseModel:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<BaseModelVo>> list(BaseModelVo baseModel) {
        log.info("管理后台用户[{}]分页查询基础车型信息", SecurityUtils.getUsername());
        startPage();
        List<BaseModelVo> baseModelVoList = baseModelAppService.search(baseModel.getPlatformCode(), baseModel.getSeriesCode(),
                baseModel.getModelCode(), baseModel.getCode(), baseModel.getName(), getBeginTime(baseModel), getEndTime(baseModel));
        return ApiResponse.ok(getPageResult(baseModelVoList));
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
    public ApiResponse<List<BaseModelFeatureCodeVo>> listFeatureCode(@PathVariable String baseModelCode, BaseModelFeatureCodeVo baseModelFeatureCode) {
        log.info("管理后台用户[{}]分页查询基础车型下特征值", SecurityUtils.getUsername());
        List<BaseModelFeatureCodeVo> baseModelFeatureCodeVoList = baseModelAppService.searchFeatureCode(baseModelCode,
                baseModelFeatureCode.getFamilyCode(), getBeginTime(baseModelFeatureCode), getEndTime(baseModelFeatureCode));
        baseModelFeatureCodeVoList.forEach(mpt -> {
            FeatureFamilyVo featureFamily = featureFamilyAppService.getFeatureFamilyByCode(mpt.getFamilyCode());
            if (featureFamily != null) {
                mpt.setFamilyName(featureFamily.getName());
            }
            mpt.setFeatureName(new String[mpt.getFeatureCode().length]);
            int i = 0;
            for (String code : mpt.getFeatureCode()) {
                FeatureCodeVo featureCode = featureFamilyAppService.getFeatureCodeByCode(code);
                if (featureCode != null) {
                    mpt.getFeatureName()[i] = featureCode.getName();
                }
                i++;
            }

        });
        return ApiResponse.ok(baseModelFeatureCodeVoList);
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
    public ApiResponse<List<BaseModelVo>> listByPlatformCodeAndSeriesCodeAndModelCode(@RequestParam String platformCode,
                                                                                       @RequestParam String seriesCode,
                                                                                       @RequestParam String modelCode) {
        log.info("管理后台用户[{}]获取指定车辆平台[{}]及车系[{}]及车型[{}]下的所有基础车型", SecurityUtils.getUsername(),
                platformCode, seriesCode, modelCode);
        List<BaseModelVo> baseModelVoList = baseModelAppService.search(platformCode, seriesCode, modelCode,
                null, null, null, null);
        return ApiResponse.ok(baseModelVoList);
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
    public void export(HttpServletResponse response, BaseModelVo baseModel) {
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
    public ApiResponse<BaseModelVo> getInfo(@PathVariable Long baseModelId) {
        log.info("管理后台用户[{}]根据基础车型ID[{}]获取基础车型信息", SecurityUtils.getUsername(), baseModelId);
        return ApiResponse.ok(baseModelAppService.getBaseModelById(baseModelId));
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
    public ApiResponse<BaseModelFeatureCodeVo> getFeatureCodeInfo(@PathVariable String baseModelCode, @PathVariable Long baseModelFeatureCodeId) {
        log.info("管理后台用户[{}]根据基础车型[{}]特征值ID[{}]获取基础车型特征值信息", SecurityUtils.getUsername(), baseModelCode, baseModelFeatureCodeId);
        return ApiResponse.ok(baseModelAppService.getBaseModelFeatureCodeById(baseModelFeatureCodeId));
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
    public ApiResponse<Void> add(@Validated @RequestBody BaseModelVo baseModel) {
        log.info("管理后台用户[{}]新增基础车型信息[{}]", SecurityUtils.getUsername(), baseModel.getCode());
        if (!baseModelAppService.checkCodeUnique(baseModel.getId(), baseModel.getCode())) {
            return ApiResponse.fail("新增基础车型'" + baseModel.getCode() + "'失败，基础车型代码已存在");
        }
        return baseModelAppService.createBasicModel(baseModel, SecurityUtils.getUserId().toString()) > 0 ? ApiResponse.ok() : ApiResponse.fail("新增失败");
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
    public ApiResponse<Void> addFeatureCode(@PathVariable String baseModelCode, @Validated @RequestBody BaseModelFeatureCodeVo baseModelFeatureCode) {
        log.info("管理后台用户[{}]新增基础车型[{}]特征值[{}]", SecurityUtils.getUsername(), baseModelCode, baseModelFeatureCode.getFamilyCode());
        if (!baseModelAppService.checkFeatureCodeUnique(baseModelFeatureCode.getId(), baseModelCode, baseModelFeatureCode.getFamilyCode())) {
            return ApiResponse.fail("新增基础车型特征值'" + baseModelFeatureCode.getFamilyCode() + "'失败，基础车型特征值已存在");
        }
        return baseModelAppService.createBasicModelFeatureCode(baseModelFeatureCode, SecurityUtils.getUserId().toString()) > 0 ? ApiResponse.ok() : ApiResponse.fail("新增失败");
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
    public ApiResponse<Void> edit(@Validated @RequestBody BaseModelVo baseModel) {
        log.info("管理后台用户[{}]修改保存基础车型信息[{}]", SecurityUtils.getUsername(), baseModel.getCode());
        if (!baseModelAppService.checkCodeUnique(baseModel.getId(), baseModel.getCode())) {
            return ApiResponse.fail("修改保存基础车型'" + baseModel.getCode() + "'失败，基础车型代码已存在");
        }
        return baseModelAppService.modifyBasicModel(baseModel, SecurityUtils.getUserId().toString()) > 0 ? ApiResponse.ok() : ApiResponse.fail("修改失败");
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
    public ApiResponse<Void> editFeatureCode(@PathVariable String baseModelCode, @Validated @RequestBody BaseModelFeatureCodeVo baseModelFeatureCode) {
        log.info("管理后台用户[{}]修改保存基础车型[{}]特征值[{}]", SecurityUtils.getUsername(), baseModelCode, baseModelFeatureCode.getFamilyCode());
        if (!baseModelAppService.checkFeatureCodeUnique(baseModelFeatureCode.getId(), baseModelCode, baseModelFeatureCode.getFamilyCode())) {
            return ApiResponse.fail("修改保存基础车型特征值'" + baseModelFeatureCode.getFamilyCode() + "'失败，基础车型特征值已存在");
        }
        return baseModelAppService.modifyBaseModelFeatureCode(baseModelFeatureCode, SecurityUtils.getUserId().toString()) > 0 ? ApiResponse.ok() : ApiResponse.fail("修改失败");
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
