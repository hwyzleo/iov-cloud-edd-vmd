package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.FeatureCodeVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.FeatureFamilyVo;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptFeatureAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.FeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.FeatureCodeQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.FeatureFamilyDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.FeatureFamilyQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.FeatureFamilyAppService;
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
 * 特征相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/feature/v1")
public class MptFeatureFamilyController extends BaseController {

    private final FeatureFamilyAppService featureFamilyAppService;

    // ==================== 特征族 ====================

    /**
     * 分页查询特征族信息
     *
     * @param featureFamily 特征族信息
     * @return 特征族信息列表
     */
    @RequiresPermissions("completeVehicle:product:feature:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<FeatureFamilyVo>> list(FeatureFamilyVo featureFamily) {
        log.info("管理后台用户[{}]分页查询特征族信息", SecurityUtils.getUsername());
        startPage();
        FeatureFamilyQuery query = FeatureFamilyQuery.builder()
                .code(featureFamily.getCode())
                .name(featureFamily.getName())
                .type(featureFamily.getType())
                .beginTime(getBeginTime(featureFamily))
                .endTime(getEndTime(featureFamily))
                .build();
        List<FeatureFamilyDto> dtoList = featureFamilyAppService.search(query);
        return ApiResponse.ok(getPageResult(MptFeatureAssembler.INSTANCE.fromFamilyDtoList(dtoList)));
    }

    /**
     * 获取所有特征族
     *
     * @return 特征族信息列表
     */
    @RequiresPermissions("completeVehicle:product:feature:list")
    @GetMapping(value = "/listAll")
    public ApiResponse<List<FeatureFamilyVo>> listAll() {
        log.info("管理后台用户[{}]获取所有特征族", SecurityUtils.getUsername());
        FeatureFamilyQuery query = FeatureFamilyQuery.builder().build();
        List<FeatureFamilyDto> dtoList = featureFamilyAppService.search(query);
        return ApiResponse.ok(MptFeatureAssembler.INSTANCE.fromFamilyDtoList(dtoList));
    }

    /**
     * 导出特征族信息
     *
     * @param response      响应
     * @param featureFamily 特征族信息
     */
    @Log(title = "特征族管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:product:feature:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, FeatureFamilyVo featureFamily) {
        log.info("管理后台用户[{}]导出特征族信息", SecurityUtils.getUsername());
    }

    /**
     * 根据特征族ID获取特征族信息
     *
     * @param featureFamilyId 特征族ID
     * @return 特征族信息
     */
    @RequiresPermissions("completeVehicle:product:feature:query")
    @GetMapping(value = "/{featureFamilyId}")
    public ApiResponse<FeatureFamilyVo> getInfo(@PathVariable Long featureFamilyId) {
        log.info("管理后台用户[{}]根据特征族ID[{}]获取特征族信息", SecurityUtils.getUsername(), featureFamilyId);
        return ApiResponse.ok(MptFeatureAssembler.INSTANCE.fromFamilyDto(featureFamilyAppService.getFeatureFamilyById(featureFamilyId)));
    }

    /**
     * 新增特征族信息
     *
     * @param featureFamily 特征族信息
     * @return 结果
     */
    @Log(title = "特征族管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:feature:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody FeatureFamilyVo featureFamily) {
        log.info("管理后台用户[{}]新增特征族信息[{}]", SecurityUtils.getUsername(), featureFamily.getCode());
        if (!featureFamilyAppService.checkFamilyCodeUnique(featureFamily.getId(), featureFamily.getCode())) {
            return ApiResponse.fail("新增特征族'" + featureFamily.getCode() + "'失败，特征族代码已存在");
        }
        featureFamilyAppService.createFeatureFamily(MptFeatureAssembler.INSTANCE.toFamilyDto(featureFamily), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 修改保存特征族信息
     *
     * @param featureFamily 特征族信息
     * @return 结果
     */
    @Log(title = "特征族管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:feature:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody FeatureFamilyVo featureFamily) {
        log.info("管理后台用户[{}]修改保存特征族信息[{}]", SecurityUtils.getUsername(), featureFamily.getCode());
        if (!featureFamilyAppService.checkFamilyCodeUnique(featureFamily.getId(), featureFamily.getCode())) {
            return ApiResponse.fail("修改保存特征族'" + featureFamily.getCode() + "'失败，特征族代码已存在");
        }
        featureFamilyAppService.modifyFeatureFamily(MptFeatureAssembler.INSTANCE.toFamilyDto(featureFamily), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 删除特征族信息
     *
     * @param featureFamilyIds 特征族ID数组
     * @return 结果
     */
    @Log(title = "特征族管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:product:feature:remove")
    @DeleteMapping("/{featureFamilyIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] featureFamilyIds) {
        log.info("管理后台用户[{}]删除特征族信息[{}]", SecurityUtils.getUsername(), featureFamilyIds);
        return featureFamilyAppService.deleteFeatureFamilyByIds(featureFamilyIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

    // ==================== 特征值 ====================

    /**
     * 查询特征族下特征值
     *
     * @param featureFamilyId 特征族ID
     * @param featureCode     特征值信息
     * @return 特征值信息列表
     */
    @RequiresPermissions("completeVehicle:product:feature:list")
    @GetMapping(value = "/{featureFamilyId}/featureCode/list")
    public ApiResponse<PageResult<FeatureCodeVo>> listFeatureCode(@PathVariable Long featureFamilyId, FeatureCodeVo featureCode) {
        log.info("管理后台用户[{}]查询特征族[{}]下特征值", SecurityUtils.getUsername(), featureFamilyId);
        startPage();
        FeatureCodeQuery query = FeatureCodeQuery.builder()
                .featureFamilyId(featureFamilyId)
                .familyCode(featureCode.getFamilyCode())
                .name(featureCode.getName())
                .featureCode(featureCode.getCode())
                .beginTime(getBeginTime(featureCode))
                .endTime(getEndTime(featureCode))
                .build();
        List<FeatureCodeDto> dtoList = featureFamilyAppService.searchFeatureCode(query);
        return ApiResponse.ok(getPageResult(MptFeatureAssembler.INSTANCE.fromCodeDtoList(dtoList)));
    }

    /**
     * 根据特征值ID获取特征值信息
     *
     * @param featureFamilyId 特征族ID
     * @param featureCodeId   特征值ID
     * @return 特征值信息
     */
    @RequiresPermissions("completeVehicle:product:feature:query")
    @GetMapping(value = "/{featureFamilyId}/featureCode/{featureCodeId}")
    public ApiResponse<FeatureCodeVo> getFeatureCodeInfo(@PathVariable Long featureFamilyId, @PathVariable Long featureCodeId) {
        log.info("管理后台用户[{}]根据特征值ID[{}]获取特征值信息", SecurityUtils.getUsername(), featureCodeId);
        return ApiResponse.ok(MptFeatureAssembler.INSTANCE.fromCodeDto(featureFamilyAppService.getFeatureCodeById(featureFamilyId, featureCodeId)));
    }

    /**
     * 新增特征值信息
     *
     * @param featureFamilyId 特征族ID
     * @param featureCode     特征值信息
     * @return 结果
     */
    @Log(title = "特征值管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:feature:add")
    @PostMapping("/{featureFamilyId}/featureCode")
    public ApiResponse<Void> addFeatureCode(@PathVariable Long featureFamilyId, @Validated @RequestBody FeatureCodeVo featureCode) {
        log.info("管理后台用户[{}]新增特征族[{}]下特征值信息[{}]", SecurityUtils.getUsername(), featureFamilyId, featureCode.getCode());
        if (!featureFamilyAppService.checkFeatureCodeUnique(featureCode.getId(), featureCode.getCode())) {
            return ApiResponse.fail("新增特征值'" + featureCode.getCode() + "'失败，特征值代码已存在");
        }
        featureFamilyAppService.createFeatureCode(featureFamilyId, MptFeatureAssembler.INSTANCE.toCodeDto(featureCode), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 修改保存特征值信息
     *
     * @param featureFamilyId 特征族ID
     * @param featureCode     特征值信息
     * @return 结果
     */
    @Log(title = "特征值管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:feature:edit")
    @PutMapping("/{featureFamilyId}/featureCode")
    public ApiResponse<Void> editFeatureCode(@PathVariable Long featureFamilyId, @Validated @RequestBody FeatureCodeVo featureCode) {
        log.info("管理后台用户[{}]修改保存特征族[{}]下特征值信息[{}]", SecurityUtils.getUsername(), featureFamilyId, featureCode.getCode());
        if (!featureFamilyAppService.checkFeatureCodeUnique(featureCode.getId(), featureCode.getCode())) {
            return ApiResponse.fail("修改保存特征值'" + featureCode.getCode() + "'失败，特征值代码已存在");
        }
        featureFamilyAppService.modifyFeatureCode(featureFamilyId, MptFeatureAssembler.INSTANCE.toCodeDto(featureCode), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 删除特征值信息
     *
     * @param featureFamilyId 特征族ID
     * @param featureCodeIds  特征值ID数组
     * @return 结果
     */
    @Log(title = "特征值管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:product:feature:remove")
    @DeleteMapping("/{featureFamilyId}/featureCode/{featureCodeIds}")
    public ApiResponse<Void> removeFeatureCode(@PathVariable Long featureFamilyId, @PathVariable Long[] featureCodeIds) {
        log.info("管理后台用户[{}]删除特征族[{}]下特征值信息[{}]", SecurityUtils.getUsername(), featureFamilyId, featureCodeIds);
        return featureFamilyAppService.deleteFeatureCodeByIds(featureFamilyId, featureCodeIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

}
