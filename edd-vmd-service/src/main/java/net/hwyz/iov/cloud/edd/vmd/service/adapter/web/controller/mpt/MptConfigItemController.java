package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ConfigItemMappingRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ConfigItemMappingResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ConfigItemOptionRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ConfigItemOptionResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ConfigItemRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ConfigItemResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptConfigItemAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigItemDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigItemMappingDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigItemOptionDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.ConfigItemQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ConfigItemAppService;
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
 * 配置项相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/configItem/v1")
public class MptConfigItemController extends BaseController {

    private final ConfigItemAppService configItemAppService;

    // ==================== 配置项 ====================

    /**
     * 分页查询配置项信息
     *
     * @param configItem 配置项信息
     * @return 配置项信息列表
     */
    @RequiresPermissions("completeVehicle:product:configItem:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<ConfigItemResponse>> list(ConfigItemRequest configItem) {
        log.info("管理后台用户[{}]分页查询配置项信息", SecurityUtils.getUsername());
        startPage();
        ConfigItemQuery query = ConfigItemQuery.builder()
                .family(configItem.getFamily())
                .code(configItem.getCode())
                .name(configItem.getName())
                .beginTime(getBeginTime(configItem))
                .endTime(getEndTime(configItem))
                .build();
        List<ConfigItemDto> dtoList = configItemAppService.search(query);
        return ApiResponse.ok(getPageResult(MptConfigItemAssembler.INSTANCE.fromItemDtoList(dtoList)));
    }

    /**
     * 获取所有配置项
     *
     * @return 配置项列表
     */
    @RequiresPermissions("completeVehicle:product:configItem:list")
    @GetMapping(value = "/listAll")
    public ApiResponse<List<ConfigItemResponse>> listAll() {
        log.info("管理后台用户[{}]获取所有配置项", SecurityUtils.getUsername());
        List<ConfigItemDto> dtoList = configItemAppService.listAll();
        return ApiResponse.ok(MptConfigItemAssembler.INSTANCE.fromItemDtoList(dtoList));
    }

    /**
     * 导出配置项信息
     *
     * @param response   响应
     * @param configItem 配置项信息
     */
    @Log(title = "配置项管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:product:configItem:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, ConfigItemRequest configItem) {
        log.info("管理后台用户[{}]导出配置项信息", SecurityUtils.getUsername());
    }

    /**
     * 根据配置项ID获取配置项信息
     *
     * @param configItemId 配置项ID
     * @return 配置项信息
     */
    @RequiresPermissions("completeVehicle:product:configItem:query")
    @GetMapping(value = "/{configItemId}")
    public ApiResponse<ConfigItemResponse> getInfo(@PathVariable Long configItemId) {
        log.info("管理后台用户[{}]根据配置项ID[{}]获取配置项信息", SecurityUtils.getUsername(), configItemId);
        return ApiResponse.ok(MptConfigItemAssembler.INSTANCE.fromItemDto(configItemAppService.getConfigItemById(configItemId)));
    }

    /**
     * 新增配置项信息
     *
     * @param configItem 配置项信息
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:product:configItem:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody ConfigItemRequest configItem) {
        log.info("管理后台用户[{}]新增配置项信息[{}]", SecurityUtils.getUsername(), configItem.getCode());
        if (!configItemAppService.checkCodeUnique(configItem.getId(), configItem.getCode())) {
            return ApiResponse.fail("新增配置项'" + configItem.getCode() + "'失败，配置项代码已存在");
        }
        configItemAppService.createConfigItem(MptConfigItemAssembler.INSTANCE.toItemCmd(configItem), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 修改保存配置项信息
     *
     * @param configItem 配置项信息
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:configItem:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody ConfigItemRequest configItem) {
        log.info("管理后台用户[{}]修改保存配置项信息[{}]", SecurityUtils.getUsername(), configItem.getCode());
        if (!configItemAppService.checkCodeUnique(configItem.getId(), configItem.getCode())) {
            return ApiResponse.fail("修改保存配置项'" + configItem.getCode() + "'失败，配置项代码已存在");
        }
        configItemAppService.modifyConfigItem(MptConfigItemAssembler.INSTANCE.toItemCmd(configItem), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 删除配置项信息
     *
     * @param configItemIds 配置项ID数组
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:product:configItem:remove")
    @DeleteMapping("/{configItemIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] configItemIds) {
        log.info("管理后台用户[{}]删除配置项信息[{}]", SecurityUtils.getUsername(), configItemIds);
        return configItemAppService.deleteConfigItemByIds(configItemIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

    // ==================== 枚举值 ====================

    /**
     * 获取配置项下枚举值
     *
     * @param configItemCode 配置项编码
     * @return 枚举值信息列表
     */
    @RequiresPermissions("completeVehicle:product:configItem:list")
    @GetMapping(value = "/{configItemCode}/option/list")
    public ApiResponse<List<ConfigItemOptionResponse>> listOption(@PathVariable String configItemCode) {
        log.info("管理后台用户[{}]查询配置项[{}]下枚举值", SecurityUtils.getUsername(), configItemCode);
        List<ConfigItemOptionDto> dtoList = configItemAppService.listOption(configItemCode);
        return ApiResponse.ok(MptConfigItemAssembler.INSTANCE.fromOptionDtoList(dtoList));
    }

    /**
     * 根据枚举值ID获取枚举值信息
     *
     * @param configItemCode 配置项编码
     * @param optionId       枚举值ID
     * @return 枚举值信息
     */
    @RequiresPermissions("completeVehicle:product:configItem:query")
    @GetMapping(value = "/{configItemCode}/option/{optionId}")
    public ApiResponse<ConfigItemOptionResponse> getOptionInfo(@PathVariable String configItemCode, @PathVariable Long optionId) {
        log.info("管理后台用户[{}]根据枚举值ID[{}]获取枚举值信息", SecurityUtils.getUsername(), optionId);
        return ApiResponse.ok(MptConfigItemAssembler.INSTANCE.fromOptionDto(configItemAppService.getOptionById(optionId)));
    }

    /**
     * 新增枚举值信息
     *
     * @param configItemCode 配置项编码
     * @param option         枚举值信息
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:configItem:edit")
    @PostMapping("/{configItemCode}/option")
    public ApiResponse<Void> addOption(@PathVariable String configItemCode, @Validated @RequestBody ConfigItemOptionRequest option) {
        log.info("管理后台用户[{}]新增配置项[{}]下枚举值信息[{}]", SecurityUtils.getUsername(), configItemCode, option.getCode());
        configItemAppService.createOption(MptConfigItemAssembler.INSTANCE.toOptionCmd(option), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 修改保存枚举值信息
     *
     * @param configItemCode 配置项编码
     * @param option         枚举值信息
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:configItem:edit")
    @PutMapping("/{configItemCode}/option")
    public ApiResponse<Void> editOption(@PathVariable String configItemCode, @Validated @RequestBody ConfigItemOptionRequest option) {
        log.info("管理后台用户[{}]修改保存配置项[{}]下枚举值信息[{}]", SecurityUtils.getUsername(), configItemCode, option.getCode());
        configItemAppService.modifyOption(MptConfigItemAssembler.INSTANCE.toOptionCmd(option), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 删除枚举值信息
     *
     * @param configItemCode 配置项编码
     * @param optionIds      枚举值ID数组
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:configItem:edit")
    @DeleteMapping("/{configItemCode}/option/{optionIds}")
    public ApiResponse<Void> removeOption(@PathVariable String configItemCode, @PathVariable Long[] optionIds) {
        log.info("管理后台用户[{}]删除配置项[{}]下枚举值信息[{}]", SecurityUtils.getUsername(), configItemCode, optionIds);
        return configItemAppService.deleteOptionByIds(optionIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

    // ==================== 映射 ====================

    /**
     * 获取配置项下映射
     *
     * @param configItemCode 配置项编码
     * @return 映射信息列表
     */
    @RequiresPermissions("completeVehicle:product:configItem:list")
    @GetMapping(value = "/{configItemCode}/mapping/list")
    public ApiResponse<List<ConfigItemMappingResponse>> listMapping(@PathVariable String configItemCode) {
        log.info("管理后台用户[{}]查询配置项[{}]下映射", SecurityUtils.getUsername(), configItemCode);
        List<ConfigItemMappingDto> dtoList = configItemAppService.listMapping(configItemCode);
        return ApiResponse.ok(MptConfigItemAssembler.INSTANCE.fromMappingDtoList(dtoList));
    }

    /**
     * 根据映射ID获取映射信息
     *
     * @param configItemCode 配置项编码
     * @param mappingId      映射ID
     * @return 映射信息
     */
    @RequiresPermissions("completeVehicle:product:configItem:query")
    @GetMapping(value = "/{configItemCode}/mapping/{mappingId}")
    public ApiResponse<ConfigItemMappingResponse> getMappingInfo(@PathVariable String configItemCode, @PathVariable Long mappingId) {
        log.info("管理后台用户[{}]根据映射ID[{}]获取映射信息", SecurityUtils.getUsername(), mappingId);
        return ApiResponse.ok(MptConfigItemAssembler.INSTANCE.fromMappingDto(configItemAppService.getMappingById(mappingId)));
    }

    /**
     * 新增映射信息
     *
     * @param configItemCode 配置项编码
     * @param mapping        映射信息
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:configItem:edit")
    @PostMapping("/{configItemCode}/mapping")
    public ApiResponse<Void> addMapping(@PathVariable String configItemCode, @Validated @RequestBody ConfigItemMappingRequest mapping) {
        log.info("管理后台用户[{}]新增配置项[{}]下映射信息[{}]", SecurityUtils.getUsername(), configItemCode, mapping.getSourceCode());
        configItemAppService.createMapping(MptConfigItemAssembler.INSTANCE.toMappingCmd(mapping), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 修改保存映射信息
     *
     * @param configItemCode 配置项编码
     * @param mapping        映射信息
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:configItem:edit")
    @PutMapping("/{configItemCode}/mapping")
    public ApiResponse<Void> editMapping(@PathVariable String configItemCode, @Validated @RequestBody ConfigItemMappingRequest mapping) {
        log.info("管理后台用户[{}]修改保存配置项[{}]下映射信息[{}]", SecurityUtils.getUsername(), configItemCode, mapping.getSourceCode());
        configItemAppService.modifyMapping(MptConfigItemAssembler.INSTANCE.toMappingCmd(mapping), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 删除映射信息
     *
     * @param configItemCode 配置项编码
     * @param mappingIds     映射ID数组
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:product:configItem:edit")
    @DeleteMapping("/{configItemCode}/mapping/{mappingIds}")
    public ApiResponse<Void> removeMapping(@PathVariable String configItemCode, @PathVariable Long[] mappingIds) {
        log.info("管理后台用户[{}]删除配置项[{}]下映射信息[{}]", SecurityUtils.getUsername(), configItemCode, mappingIds);
        return configItemAppService.deleteMappingByIds(mappingIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("删除失败");
    }

}
