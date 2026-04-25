package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.ConfigItemMappingVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.ConfigItemOptionVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.ConfigItemVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ConfigItemAppService;
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

    /**
     * 分页查询配置项信息
     *
     * @param configItem 配置项信息
     * @return 配置项信息列表
     */
    @RequiresPermissions("iov:configCenter:configItem:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<ConfigItemVo>> list(ConfigItemVo configItem) {
        log.info("管理后台用户[{}]分页查询配置项信息", SecurityUtils.getUsername());
        startPage();
        List<ConfigItemVo> configItemVoList = configItemAppService.search(configItem.getCode(), configItem.getName(),
                getBeginTime(configItem), getEndTime(configItem));
        return ApiResponse.ok(getPageResult(configItemVoList));
    }

    /**
     * 查询配置项枚举值信息
     *
     * @param configItemCode   配置项代码
     * @param configItemOption 配置项枚举值信息
     * @return 配置项枚举值信息列表
     */
    @RequiresPermissions("iov:configCenter:configItem:list")
    @GetMapping(value = "/{configItemCode}/option/list")
    public ApiResponse listOption(@PathVariable String configItemCode, ConfigItemOptionVo configItemOption) {
        log.info("管理后台用户[{}]查询配置项[{}]枚举值信息", SecurityUtils.getUsername(), configItemCode);
        List<ConfigItemOptionVo> configItemOptionVoList = configItemAppService.searchOption(configItemCode, configItemOption.getCode(),
                configItemOption.getName(), getBeginTime(configItemOption), getEndTime(configItemOption));
        return ApiResponse.ok(configItemOptionVoList);
    }

    /**
     * 查询配置项映射信息
     *
     * @param configItemCode    配置项代码
     * @param configItemMapping 配置项映射信息
     * @return 配置项映射信息列表
     */
    @RequiresPermissions("iov:configCenter:configItem:list")
    @GetMapping(value = "/{configItemCode}/mapping/list")
    public ApiResponse listMapping(@PathVariable String configItemCode, ConfigItemMappingVo configItemMapping) {
        log.info("管理后台用户[{}]查询配置项[{}]映射信息", SecurityUtils.getUsername(), configItemCode);
        List<ConfigItemMappingVo> configItemMappingVoList = configItemAppService.searchMapping(configItemCode,
                configItemMapping.getSourceSystem(), getBeginTime(configItemMapping), getEndTime(configItemMapping));
        return ApiResponse.ok(configItemMappingVoList);
    }

    /**
     * 导出配置项信息
     *
     * @param response   响应
     * @param configItem 配置项信息
     */
    @Log(title = "配置项管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("iov:configCenter:configItem:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, ConfigItemVo configItem) {
        log.info("管理后台用户[{}]导出配置项信息", SecurityUtils.getUsername());
    }

    /**
     * 根据配置项ID获取配置项信息
     *
     * @param configItemId 配置项ID
     * @return 配置项信息
     */
    @RequiresPermissions("iov:configCenter:configItem:query")
    @GetMapping(value = "/{configItemId}")
    public ApiResponse getInfo(@PathVariable Long configItemId) {
        log.info("管理后台用户[{}]根据配置项ID[{}]获取配置项信息", SecurityUtils.getUsername(), configItemId);
        return ApiResponse.ok(configItemAppService.getConfigItemById(configItemId));
    }

    /**
     * 根据配置项枚举值ID获取配置项枚举值信息
     *
     * @param configItemCode     配置项代码
     * @param configItemOptionId 配置项枚举值ID
     * @return 配置项枚举值信息
     */
    @RequiresPermissions("iov:configCenter:configItem:query")
    @GetMapping(value = "/{configItemCode}/option/{configItemOptionId}")
    public ApiResponse getOptionInfo(@PathVariable String configItemCode, @PathVariable Long configItemOptionId) {
        log.info("管理后台用户[{}]根据配置项[{}]枚举值ID[{}]获取配置项枚举值信息", SecurityUtils.getUsername(), configItemCode, configItemOptionId);
        return ApiResponse.ok(configItemAppService.getConfigItemOptionById(configItemCode, configItemOptionId));
    }

    /**
     * 根据配置项映射ID获取配置项映射信息
     *
     * @param configItemCode      配置项代码
     * @param configItemMappingId 配置项映射ID
     * @return 配置项映射信息
     */
    @RequiresPermissions("iov:configCenter:configItem:query")
    @GetMapping(value = "/{configItemCode}/mapping/{configItemMappingId}")
    public ApiResponse getMappingInfo(@PathVariable String configItemCode, @PathVariable Long configItemMappingId) {
        log.info("管理后台用户[{}]根据配置项[{}]映射ID[{}]获取配置项映射信息", SecurityUtils.getUsername(), configItemCode, configItemMappingId);
        return ApiResponse.ok(configItemAppService.getConfigItemMappingById(configItemCode, configItemMappingId));
    }

    /**
     * 新增配置项信息
     *
     * @param configItem 配置项信息
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("iov:configCenter:configItem:add")
    @PostMapping
    public ApiResponse add(@Validated @RequestBody ConfigItemVo configItem) {
        log.info("管理后台用户[{}]新增配置项信息[{}]", SecurityUtils.getUsername(), configItem.getCode());
        if (!configItemAppService.checkCodeUnique(configItem.getId(), configItem.getCode())) {
            return ApiResponse.fail("新增配置项'" + configItem.getCode() + "'失败，配置项代码已存在");
        }
        return configItemAppService.createConfigItem(configItem, SecurityUtils.getUserId().toString()) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

    /**
     * 新增配置项枚举值信息
     *
     * @param configItemCode   配置项代码
     * @param configItemOption 配置项枚举值信息
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("iov:configCenter:configItem:edit")
    @PostMapping("/{configItemCode}/option")
    public ApiResponse addOption(@PathVariable String configItemCode, @Validated @RequestBody ConfigItemOptionVo configItemOption) {
        log.info("管理后台用户[{}]新增配置项[{}]枚举值信息[{}]", SecurityUtils.getUsername(), configItemCode, configItemOption.getCode());
        if (!configItemAppService.checkOptionCodeUnique(configItemOption.getId(), configItemCode, configItemOption.getCode())) {
            return ApiResponse.fail("新增配置项枚举值'" + configItemOption.getCode() + "'失败，配置项枚举值代码已存在");
        }
        return configItemAppService.createConfigItemOption(configItemCode, configItemOption, SecurityUtils.getUserId().toString()) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

    /**
     * 新增配置项映射信息
     *
     * @param configItemCode    配置项代码
     * @param configItemMapping 配置项映射信息
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("iov:configCenter:configItem:edit")
    @PostMapping("/{configItemCode}/mapping")
    public ApiResponse addMapping(@PathVariable String configItemCode, @Validated @RequestBody ConfigItemMappingVo configItemMapping) {
        log.info("管理后台用户[{}]新增配置项[{}]映射信息[{}:{}]", SecurityUtils.getUsername(), configItemCode,
                configItemMapping.getSourceSystem(), configItemMapping.getSourceCode());
        if (!configItemAppService.checkMappingCodeUnique(configItemMapping.getId(), configItemCode, configItemMapping.getSourceSystem(),
                configItemMapping.getSourceCode(), configItemMapping.getSourceValue())) {
            return ApiResponse.fail("新增配置项映射'" + configItemMapping.getSourceSystem() + "'失败，配置项映射已存在");
        }
        return configItemAppService.createConfigItemMapping(configItemCode, configItemMapping, SecurityUtils.getUserId().toString()) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

    /**
     * 修改保存配置项信息
     *
     * @param configItem 配置项信息
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("iov:configCenter:configItem:edit")
    @PutMapping
    public ApiResponse edit(@Validated @RequestBody ConfigItemVo configItem) {
        log.info("管理后台用户[{}]修改保存配置项信息[{}]", SecurityUtils.getUsername(), configItem.getCode());
        if (!configItemAppService.checkCodeUnique(configItem.getId(), configItem.getCode())) {
            return ApiResponse.fail("修改保存配置项'" + configItem.getCode() + "'失败，配置项代码已存在");
        }
        return configItemAppService.modifyConfigItem(configItem, SecurityUtils.getUserId().toString()) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

    /**
     * 修改保存配置项枚举值信息
     *
     * @param configItemCode   配置项代码
     * @param configItemOption 配置项枚举值信息
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("iov:configCenter:configItem:edit")
    @PutMapping("/{configItemCode}/option")
    public ApiResponse editOption(@PathVariable String configItemCode, @Validated @RequestBody ConfigItemOptionVo configItemOption) {
        log.info("管理后台用户[{}]修改保存配置项[{}]枚举值信息[{}]", SecurityUtils.getUsername(), configItemCode, configItemOption.getCode());
        if (!configItemAppService.checkOptionCodeUnique(configItemOption.getId(), configItemCode, configItemOption.getCode())) {
            return ApiResponse.fail("修改保存配置项枚举值'" + configItemOption.getCode() + "'失败，配置项枚举值代码已存在");
        }
        return configItemAppService.modifyConfigItemOption(configItemCode, configItemOption, SecurityUtils.getUserId().toString()) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

    /**
     * 修改保存配置项映射信息
     *
     * @param configItemCode    配置项代码
     * @param configItemMapping 配置项映射信息
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("iov:configCenter:configItem:edit")
    @PutMapping("/{configItemCode}/mapping")
    public ApiResponse editMapping(@PathVariable String configItemCode, @Validated @RequestBody ConfigItemMappingVo configItemMapping) {
        log.info("管理后台用户[{}]修改保存配置项[{}]映射信息[{}:{}]", SecurityUtils.getUsername(), configItemCode,
                configItemMapping.getSourceSystem(), configItemMapping.getSourceCode());
        if (!configItemAppService.checkMappingCodeUnique(configItemMapping.getId(), configItemCode, configItemMapping.getSourceSystem(),
                configItemMapping.getSourceCode(), configItemMapping.getSourceValue())) {
            return ApiResponse.fail("修改保存配置项映射'" + configItemMapping.getSourceSystem() + "'失败，配置项映射代码已存在");
        }
        return configItemAppService.modifyConfigItemMapping(configItemCode, configItemMapping, SecurityUtils.getUserId().toString()) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

    /**
     * 删除配置项信息
     *
     * @param configItemIds 配置项ID数组
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("iov:configCenter:configItem:remove")
    @DeleteMapping("/{configItemIds}")
    public ApiResponse remove(@PathVariable Long[] configItemIds) {
        log.info("管理后台用户[{}]删除配置项信息[{}]", SecurityUtils.getUsername(), configItemIds);
        return configItemAppService.deleteConfigItemByIds(configItemIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

    /**
     * 删除配置项枚举值信息
     *
     * @param configItemCode      配置项代码
     * @param configItemOptionIds 配置项枚举值ID数组
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("iov:configCenter:configItem:edit")
    @DeleteMapping("/{configItemCode}/option/{configItemOptionIds}")
    public ApiResponse removeOption(@PathVariable String configItemCode, @PathVariable Long[] configItemOptionIds) {
        log.info("管理后台用户[{}]删除配置项[{}]枚举值信息[{}]", SecurityUtils.getUsername(), configItemCode, configItemOptionIds);
        return configItemAppService.deleteConfigItemOptionByIds(configItemCode, configItemOptionIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

    /**
     * 删除配置项映射信息
     *
     * @param configItemCode       配置项代码
     * @param configItemMappingIds 配置项映射ID数组
     * @return 结果
     */
    @Log(title = "配置项管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("iov:configCenter:configItem:edit")
    @DeleteMapping("/{configItemCode}/option/{configItemMappingIds}")
    public ApiResponse removeMapping(@PathVariable String configItemCode, @PathVariable Long[] configItemMappingIds) {
        log.info("管理后台用户[{}]删除配置项[{}]映射信息[{}]", SecurityUtils.getUsername(), configItemCode, configItemMappingIds);
        return configItemAppService.deleteConfigItemMappingByIds(configItemCode, configItemMappingIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }
}
