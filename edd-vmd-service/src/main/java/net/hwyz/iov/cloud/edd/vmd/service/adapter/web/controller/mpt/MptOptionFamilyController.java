package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptOptionAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.OptionCodeRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.OptionFamilyRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.OptionCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.OptionFamilyResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.OptionCodeQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.OptionFamilyQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OptionCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OptionFamilyDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.OptionFamilyAppService;
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
 * 选装相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/optionFamily/v1")
public class MptOptionFamilyController extends BaseController {

    private final OptionFamilyAppService optionFamilyAppService;

    // ==================== 选装族 ====================

    /**
     * 分页查询选装族信息
     *
     * @param optionFamily 选装族信息
     * @return 选装族信息列表
     */
    @RequiresPermissions("completeVehicle:product:optionFamily:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<OptionFamilyResponse>> list(OptionFamilyRequest optionFamily) {
        log.info("管理后台用户[{}]分页查询选装族信息", SecurityContextHolder.getUserName());
        startPage();
        OptionFamilyQuery query = OptionFamilyQuery.builder()
                .code(optionFamily.getCode())
                .name(optionFamily.getName())
                .type(optionFamily.getType())
                .beginTime(getBeginTime(optionFamily))
                .endTime(getEndTime(optionFamily))
                .build();
        List<OptionFamilyDto> dtoList = optionFamilyAppService.search(query);
        return ApiResponse.ok(getPageResult(PageUtil.convert(dtoList, MptOptionAssembler.INSTANCE::fromFamilyDto)));
    }

    /**
     * 获取所有选装族
     *
     * @return 选装族信息列表
     */
    @RequiresPermissions("completeVehicle:product:optionFamily:list")
    @GetMapping(value = "/listAll")
    public ApiResponse<List<OptionFamilyResponse>> listAll() {
        log.info("管理后台用户[{}]获取所有选装族", SecurityContextHolder.getUserName());
        OptionFamilyQuery query = OptionFamilyQuery.builder().build();
        List<OptionFamilyDto> dtoList = optionFamilyAppService.search(query);
        return ApiResponse.ok(MptOptionAssembler.INSTANCE.fromFamilyDtoList(dtoList));
    }

    /**
     * 导出选装族信息
     *
     * @param response      响应
     * @param optionFamily 选装族信息
     */
    @Log(title = "选装族管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:product:optionFamily:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, OptionFamilyRequest optionFamily) {
        log.info("管理后台用户[{}]导出选装族信息", SecurityContextHolder.getUserName());
    }

    /**
     * 根据选装族ID获取选装族信息
     *
     * @param optionFamilyId 选装族ID
     * @return 选装族信息
     */
    @RequiresPermissions("completeVehicle:product:optionFamily:query")
    @GetMapping(value = "/{optionFamilyId}")
    public ApiResponse<OptionFamilyResponse> getInfo(@PathVariable Long optionFamilyId) {
        log.info("管理后台用户[{}]根据选装族ID[{}]获取选装族信息", SecurityContextHolder.getUserName(), optionFamilyId);
        return ApiResponse.ok(MptOptionAssembler.INSTANCE.fromFamilyDto(optionFamilyAppService.getOptionFamilyById(optionFamilyId)));
    }

    /**
     * 新增选装族信息
     *
     * @param optionFamily 选装族信息
     * @return 结果
     */
    @Log(title = "选装族管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:optionFamily:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody OptionFamilyRequest optionFamily) {
        log.info("管理后台用户[{}]新增选装族信息[{}]", SecurityContextHolder.getUserName(), optionFamily.getCode());
        if (!optionFamilyAppService.checkOptionFamilyCodeUnique(optionFamily.getId(), optionFamily.getCode())) {
            return ApiResponse.fail("新增选装族'" + optionFamily.getCode() + "'失败，选装族代码已存在");
        }
        optionFamilyAppService.createOptionFamily(MptOptionAssembler.INSTANCE.toFamilyCmd(optionFamily), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 修改保存选装族信息
     *
     * @param optionFamily 选装族信息
     * @return 结果
     */
    @Log(title = "选装族管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:optionFamily:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody OptionFamilyRequest optionFamily) {
        log.info("管理后台用户[{}]修改保存选装族信息[{}]", SecurityContextHolder.getUserName(), optionFamily.getCode());
        if (!optionFamilyAppService.checkOptionFamilyCodeUnique(optionFamily.getId(), optionFamily.getCode())) {
            return ApiResponse.fail("修改保存选装族'" + optionFamily.getCode() + "'失败，选装族代码已存在");
        }
        optionFamilyAppService.modifyOptionFamily(MptOptionAssembler.INSTANCE.toFamilyCmd(optionFamily), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 删除选装族信息
     *
     * @param optionFamilyIds 选装族ID数组
     * @return 结果
     */
    @Log(title = "选装族管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:product:optionFamily:remove")
    @DeleteMapping("/{optionFamilyIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] optionFamilyIds) {
        log.info("管理后台用户[{}]删除选装族信息[{}]", SecurityContextHolder.getUserName(), optionFamilyIds);
        return optionFamilyAppService.deleteOptionFamilyByIds(optionFamilyIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

    // ==================== 选装值 ====================

    /**
     * 查询选装族下所有选装值
     *
     * @param optionFamilyCode 选装族代码
     * @return 选装值信息列表
     */
    @RequiresPermissions("completeVehicle:product:optionFamily:list")
    @GetMapping(value = "/listAllOptionCode")
    public ApiResponse<List<OptionCodeResponse>> listAllOptionCode(@RequestParam String optionFamilyCode) {
        log.info("管理后台用户[{}]查询选装族[{}]下选装值", SecurityContextHolder.getUserName(), optionFamilyCode);
        List<OptionCodeDto> dtoList = optionFamilyAppService.listAllOptionCodeByOptionFamilyCode(optionFamilyCode);
        return ApiResponse.ok(MptOptionAssembler.INSTANCE.fromCodeDtoList(dtoList));
    }

    /**
     * 查询选装族下选装值
     *
     * @param optionFamilyId 选装族ID
     * @param optionCode     选装值信息
     * @return 选装值信息列表
     */
    @RequiresPermissions("completeVehicle:product:optionFamily:list")
    @GetMapping(value = "/{optionFamilyId}/optionCode/list")
    public ApiResponse<PageResult<OptionCodeResponse>> listOptionCode(@PathVariable Long optionFamilyId, OptionCodeRequest optionCode) {
        log.info("管理后台用户[{}]查询选装族[{}]下选装值", SecurityContextHolder.getUserName(), optionFamilyId);
        startPage();
        OptionCodeQuery query = OptionCodeQuery.builder()
                .optionFamilyId(optionFamilyId)
                .optionFamilyCode(optionCode.getOptionFamilyCode())
                .name(optionCode.getName())
                .optionCode(optionCode.getCode())
                .beginTime(getBeginTime(optionCode))
                .endTime(getEndTime(optionCode))
                .build();
        List<OptionCodeDto> dtoList = optionFamilyAppService.searchOptionCode(query);
        return ApiResponse.ok(getPageResult(PageUtil.convert(dtoList, MptOptionAssembler.INSTANCE::fromCodeDto)));
    }

    /**
     * 根据选装值ID获取选装值信息
     *
     * @param optionFamilyId 选装族ID
     * @param optionCodeId   选装值ID
     * @return 选装值信息
     */
    @RequiresPermissions("completeVehicle:product:optionFamily:query")
    @GetMapping(value = "/{optionFamilyId}/optionCode/{optionCodeId}")
    public ApiResponse<OptionCodeResponse> getOptionCodeInfo(@PathVariable Long optionFamilyId, @PathVariable Long optionCodeId) {
        log.info("管理后台用户[{}]根据选装值ID[{}]获取选装值信息", SecurityContextHolder.getUserName(), optionCodeId);
        return ApiResponse.ok(MptOptionAssembler.INSTANCE.fromCodeDto(optionFamilyAppService.getOptionCodeById(optionFamilyId, optionCodeId)));
    }

    /**
     * 新增选装值信息
     *
     * @param optionFamilyId 选装族ID
     * @param optionCode     选装值信息
     * @return 结果
     */
    @Log(title = "选装值管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:optionFamily:add")
    @PostMapping("/{optionFamilyId}/optionCode")
    public ApiResponse<Void> addOptionCode(@PathVariable Long optionFamilyId, @Validated @RequestBody OptionCodeRequest optionCode) {
        log.info("管理后台用户[{}]新增选装族[{}]下选装值信息[{}]", SecurityContextHolder.getUserName(), optionFamilyId, optionCode.getCode());
        if (!optionFamilyAppService.checkOptionCodeUnique(optionCode.getId(), optionCode.getCode())) {
            return ApiResponse.fail("新增选装值'" + optionCode.getCode() + "'失败，选装值代码已存在");
        }
        optionFamilyAppService.createOptionCode(optionFamilyId, MptOptionAssembler.INSTANCE.toCodeCmd(optionCode), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 修改保存选装值信息
     *
     * @param optionFamilyId 选装族ID
     * @param optionCode     选装值信息
     * @return 结果
     */
    @Log(title = "选装值管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:optionFamily:edit")
    @PutMapping("/{optionFamilyId}/optionCode")
    public ApiResponse<Void> editOptionCode(@PathVariable Long optionFamilyId, @Validated @RequestBody OptionCodeRequest optionCode) {
        log.info("管理后台用户[{}]修改保存选装族[{}]下选装值信息[{}]", SecurityContextHolder.getUserName(), optionFamilyId, optionCode.getCode());
        if (!optionFamilyAppService.checkOptionCodeUnique(optionCode.getId(), optionCode.getCode())) {
            return ApiResponse.fail("修改保存选装值'" + optionCode.getCode() + "'失败，选装值代码已存在");
        }
        optionFamilyAppService.modifyOptionCode(optionFamilyId, MptOptionAssembler.INSTANCE.toCodeCmd(optionCode), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 删除选装值信息
     *
     * @param optionFamilyId 选装族ID
     * @param optionCodeIds  选装值ID数组
     * @return 结果
     */
    @Log(title = "选装值管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:product:optionFamily:remove")
    @DeleteMapping("/{optionFamilyId}/optionCode/{optionCodeIds}")
    public ApiResponse<Void> removeOptionCode(@PathVariable Long optionFamilyId, @PathVariable Long[] optionCodeIds) {
        log.info("管理后台用户[{}]删除选装族[{}]下选装值信息[{}]", SecurityContextHolder.getUserName(), optionFamilyId, optionCodeIds);
        return optionFamilyAppService.deleteOptionCodeByIds(optionFamilyId, optionCodeIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

}
