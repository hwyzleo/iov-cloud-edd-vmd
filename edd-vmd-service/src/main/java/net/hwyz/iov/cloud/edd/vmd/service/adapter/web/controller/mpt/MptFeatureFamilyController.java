package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.FeatureCodeVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.FeatureFamilyVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.FeatureFamilyAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.FeatureCodeAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.FeatureFamilyAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehFeatureCodePo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehFeatureFamilyPo;
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
 * 车辆特征族相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/featureFamily/v1")
public class MptFeatureFamilyController extends BaseController {

    private final FeatureFamilyAppService featureFamilyAppService;

    /**
     * 分页查询车辆特征族信息
     *
     * @param featureFamily 车辆特征族信息
     * @return 车辆特征族信息列表
     */
    @RequiresPermissions("completeVehicle:product:featureFamily:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<FeatureFamilyVo>> list(FeatureFamilyVo featureFamily) {
        log.info("管理后台用户[{}]分页查询车辆特征族信息", SecurityUtils.getUsername());
        startPage();
        List<FeatureFamilyVo> featureFamilyVoList = featureFamilyAppService.search(featureFamily.getCode(), featureFamily.getName(),
                featureFamily.getType(), getBeginTime(featureFamily), getEndTime(featureFamily));
        return ApiResponse.ok(getPageResult(featureFamilyVoList));
    }

    /**
     * 分页查询车辆特征族下特征值信息
     *
     * @param featureFamilyId 车辆特征族ID
     * @param featureCode     车辆特征值信息
     * @return 车辆特征族信息列表
     */
    @RequiresPermissions("completeVehicle:product:featureFamily:list")
    @GetMapping(value = "/{featureFamilyId}/featureCode/list")
    public ApiResponse<PageResult<FeatureCodeVo>> listFeatureCode(@PathVariable Long featureFamilyId, FeatureCodeVo featureCode) {
        log.info("管理后台用户[{}]分页查询车辆特征族[{}]下特征值信息", SecurityUtils.getUsername(), featureFamilyId);
        startPage();
        List<VehFeatureCodePo> featureCodePoList = featureFamilyAppService.searchFeatureCode(featureFamilyId, null,
                featureCode.getName(), featureCode.getName(), getBeginTime(featureCode), getEndTime(featureCode));
        List<FeatureCodeVo> featureCodeVoList = FeatureCodeAssembler.INSTANCE.fromPoList(featureCodePoList);
        return ApiResponse.ok(getPageResult(featureCodeVoList));
    }

    /**
     * 获取车辆特征族列表
     *
     * @return 车辆特征族列表
     */
    @RequiresPermissions("completeVehicle:product:featureFamily:list")
    @GetMapping(value = "/listAllFeatureFamily")
    public ApiResponse<List<FeatureFamilyVo>> listAllFeatureFamily() {
        log.info("管理后台用户[{}]获取车辆特征族列表", SecurityUtils.getUsername());
        List<FeatureFamilyVo> featureFamilyVoList = featureFamilyAppService.search(null, null, null, null, null);
        return ApiResponse.ok(featureFamilyVoList);
    }

    /**
     * 获取车辆特征值列表
     *
     * @param familyCode 车辆特征族代码
     * @return 车辆特征值列表
     */
    @RequiresPermissions("completeVehicle:product:featureFamily:list")
    @GetMapping(value = "/listAllFeatureCode")
    public ApiResponse<List<FeatureCodeVo>> listAllFeatureCode(@RequestParam String familyCode) {
        log.info("管理后台用户[{}]获取车辆特征族[{}]下特征值列表", SecurityUtils.getUsername(), familyCode);
        List<VehFeatureCodePo> featureCodePoList = featureFamilyAppService.searchFeatureCode(null, familyCode, null, null, null, null);
        return ApiResponse.ok(FeatureCodeAssembler.INSTANCE.fromPoList(featureCodePoList));
    }

    /**
     * 导出车辆特征族信息
     *
     * @param response      响应
     * @param featureFamily 车辆特征族信息
     */
    @Log(title = "车辆特征族管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:product:featureFamily:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, FeatureFamilyVo featureFamily) {
        log.info("管理后台用户[{}]导出车辆特征族信息", SecurityUtils.getUsername());
    }

    /**
     * 根据车辆特征族ID获取车辆特征族信息
     *
     * @param featureFamilyId 车辆特征族ID
     * @return 车辆特征族信息
     */
    @RequiresPermissions("completeVehicle:product:featureFamily:query")
    @GetMapping(value = "/{featureFamilyId}")
    public ApiResponse<FeatureFamilyVo> getInfo(@PathVariable Long featureFamilyId) {
        log.info("管理后台用户[{}]根据车辆特征族ID[{}]获取车辆特征族信息", SecurityUtils.getUsername(), featureFamilyId);
        VehFeatureFamilyPo featureFamilyPo = featureFamilyAppService.getFeatureFamilyById(featureFamilyId);
        return ApiResponse.ok(FeatureFamilyAssembler.INSTANCE.fromPo(featureFamilyPo));
    }

    /**
     * 根据车辆特征值ID获取车辆特征值信息
     *
     * @param featureFamilyId 车辆特征族ID
     * @param featureCodeId   车辆特征值ID
     * @return 车辆特征值信息
     */
    @RequiresPermissions("completeVehicle:product:featureFamily:query")
    @GetMapping(value = "/{featureFamilyId}/featureCode/{featureCodeId}")
    public ApiResponse<FeatureCodeVo> getFeatureCodeInfo(@PathVariable Long featureFamilyId, @PathVariable Long featureCodeId) {
        log.info("管理后台用户[{}]根据车辆特征值ID[{}]获取车辆特征值信息", SecurityUtils.getUsername(), featureCodeId);
        VehFeatureCodePo featureCodePo = featureFamilyAppService.getFeatureCodeById(featureFamilyId, featureCodeId);
        return ApiResponse.ok(FeatureCodeAssembler.INSTANCE.fromPo(featureCodePo));
    }

    /**
     * 新增车辆特征族信息
     *
     * @param featureFamily 车辆特征族信息
     * @return 结果
     */
    @Log(title = "车辆特征族管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:featureFamily:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody FeatureFamilyVo featureFamily) {
        log.info("管理后台用户[{}]新增车辆特征族信息[{}]", SecurityUtils.getUsername(), featureFamily.getCode());
        if (!featureFamilyAppService.checkFamilyCodeUnique(featureFamily.getId(), featureFamily.getCode())) {
            return ApiResponse.fail("新增车辆特征族'" + featureFamily.getCode() + "'失败，车辆特征族代码已存在");
        }
        VehFeatureFamilyPo featureFamilyPo = FeatureFamilyAssembler.INSTANCE.toPo(featureFamily);
        featureFamilyPo.setCreateBy(SecurityUtils.getUserId().toString());
        return featureFamilyAppService.createFeatureFamily(featureFamilyPo) > 0 ? ApiResponse.ok() : ApiResponse.fail("新增失败");
    }

    /**
     * 新增车辆特征值信息
     *
     * @param featureFamilyId 车辆特征族ID
     * @param featureCode     车辆特征值信息
     * @return 结果
     */
    @Log(title = "车辆特征族管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:featureFamily:edit")
    @PostMapping("/{featureFamilyId}/featureCode")
    public ApiResponse<Void> addFeatureCode(@PathVariable Long featureFamilyId, @Validated @RequestBody FeatureCodeVo featureCode) {
        log.info("管理后台用户[{}]新增车辆特征值信息[{}]", SecurityUtils.getUsername(), featureCode.getCode());
        if (!featureFamilyAppService.checkFeatureCodeUnique(featureCode.getId(), featureCode.getCode())) {
            return ApiResponse.fail("新增车辆特征值'" + featureCode.getCode() + "'失败，车辆特征值代码已存在");
        }
        VehFeatureCodePo featureCodePo = FeatureCodeAssembler.INSTANCE.toPo(featureCode);
        featureCodePo.setCreateBy(SecurityUtils.getUserId().toString());
        return featureFamilyAppService.createFeatureCode(featureFamilyId, featureCodePo) > 0 ? ApiResponse.ok() : ApiResponse.fail("新增失败");
    }

    /**
     * 修改保存车辆特征族信息
     *
     * @param featureFamily 车辆特征族信息
     * @return 结果
     */
    @Log(title = "车辆特征族管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:featureFamily:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody FeatureFamilyVo featureFamily) {
        log.info("管理后台用户[{}]修改保存车辆特征族信息[{}]", SecurityUtils.getUsername(), featureFamily.getCode());
        if (!featureFamilyAppService.checkFamilyCodeUnique(featureFamily.getId(), featureFamily.getCode())) {
            return ApiResponse.fail("修改保存车辆特征族'" + featureFamily.getCode() + "'失败，车辆特征族代码已存在");
        }
        VehFeatureFamilyPo featureFamilyPo = FeatureFamilyAssembler.INSTANCE.toPo(featureFamily);
        featureFamilyPo.setModifyBy(SecurityUtils.getUserId().toString());
        return featureFamilyAppService.modifyFeatureFamily(featureFamilyPo) > 0 ? ApiResponse.ok() : ApiResponse.fail("修改失败");
    }

    /**
     * 修改保存车辆特征值信息
     *
     * @param featureFamilyId 车辆特征族ID
     * @param featureCode     车辆特征值信息
     * @return 结果
     */
    @Log(title = "车辆特征族管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:featureFamily:edit")
    @PutMapping("/{featureFamilyId}/featureCode")
    public ApiResponse<Void> editFeatureCode(@PathVariable Long featureFamilyId, @Validated @RequestBody FeatureCodeVo featureCode) {
        log.info("管理后台用户[{}]修改保存车辆特征值信息[{}]", SecurityUtils.getUsername(), featureCode.getCode());
        if (!featureFamilyAppService.checkFeatureCodeUnique(featureCode.getId(), featureCode.getCode())) {
            return ApiResponse.fail("修改保存车辆特征值'" + featureCode.getCode() + "'失败，车辆特征值代码已存在");
        }
        VehFeatureCodePo featureCodePo = FeatureCodeAssembler.INSTANCE.toPo(featureCode);
        featureCodePo.setModifyBy(SecurityUtils.getUserId().toString());
        return featureFamilyAppService.modifyFeatureCode(featureFamilyId, featureCodePo) > 0 ? ApiResponse.ok() : ApiResponse.fail("修改失败");
    }

    /**
     * 删除车辆特征族信息
     *
     * @param featureFamilyIds 车辆特征族ID数组
     * @return 结果
     */
    @Log(title = "车辆特征族管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:product:featureFamily:remove")
    @DeleteMapping("/{featureFamilyIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] featureFamilyIds) {
        log.info("管理后台用户[{}]删除车辆特征族信息[{}]", SecurityUtils.getUsername(), featureFamilyIds);
        return featureFamilyAppService.deleteFeatureFamilyByIds(featureFamilyIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

    /**
     * 删除车辆特征值信息
     *
     * @param featureFamilyId 车辆特征族ID
     * @param featureCodeIds  车辆特征值ID数组
     * @return 结果
     */
    @Log(title = "车辆特征族管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:featureFamily:edit")
    @DeleteMapping("/{featureFamilyId}/featureCode/{featureCodeIds}")
    public ApiResponse<Void> removeFeatureCode(@PathVariable Long featureFamilyId, @PathVariable Long[] featureCodeIds) {
        log.info("管理后台用户[{}]删除车辆特征值信息[{}]", SecurityUtils.getUsername(), featureCodeIds);
        return featureFamilyAppService.deleteFeatureCodeByIds(featureFamilyId, featureCodeIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }
}
