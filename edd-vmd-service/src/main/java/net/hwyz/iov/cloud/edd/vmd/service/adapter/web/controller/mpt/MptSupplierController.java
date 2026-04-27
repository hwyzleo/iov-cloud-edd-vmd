package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.SupplierVo;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptSupplierAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.SupplierDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.SupplierQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.SupplierAppService;
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
 * 供应商相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/supplier/v1")
public class MptSupplierController extends BaseController {

    private final SupplierAppService supplierAppService;

    /**
     * 分页查询供应商信息
     *
     * @param supplier 供应商信息
     * @return 供应商信息列表
     */
    @RequiresPermissions("completeVehicle:vehicle:supplier:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<SupplierVo>> list(SupplierVo supplier) {
        log.info("管理后台用户[{}]分页查询供应商信息", SecurityUtils.getUsername());
        startPage();
        SupplierQuery query = SupplierQuery.builder()
                .code(supplier.getCode())
                .name(supplier.getName())
                .beginTime(getBeginTime(supplier))
                .endTime(getEndTime(supplier))
                .build();
        List<SupplierDto> dtoList = supplierAppService.search(query);
        return ApiResponse.ok(getPageResult(MptSupplierAssembler.INSTANCE.fromDtoList(dtoList)));
    }

    /**
     * 导出供应商信息
     *
     * @param response 响应
     * @param supplier 供应商信息
     */
    @Log(title = "供应商管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:vehicle:supplier:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SupplierVo supplier) {
        log.info("管理后台用户[{}]导出供应商信息", SecurityUtils.getUsername());
    }

    /**
     * 根据供应商ID获取供应商信息
     *
     * @param supplierId 供应商ID
     * @return 供应商信息
     */
    @RequiresPermissions("completeVehicle:vehicle:supplier:query")
    @GetMapping(value = "/{supplierId}")
    public ApiResponse<SupplierVo> getInfo(@PathVariable Long supplierId) {
        log.info("管理后台用户[{}]根据供应商ID[{}]获取供应商信息", SecurityUtils.getUsername(), supplierId);
        return ApiResponse.ok(MptSupplierAssembler.INSTANCE.fromDto(supplierAppService.getSupplierById(supplierId)));
    }

    /**
     * 新增供应商信息
     *
     * @param supplier 供应商信息
     * @return 结果
     */
    @Log(title = "供应商管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:vehicle:supplier:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody SupplierVo supplier) {
        log.info("管理后台用户[{}]新增供应商信息[{}]", SecurityUtils.getUsername(), supplier.getCode());
        if (!supplierAppService.checkCodeUnique(supplier.getId(), supplier.getCode())) {
            return ApiResponse.fail("新增供应商'" + supplier.getCode() + "'失败，供应商代码已存在");
        }
        supplierAppService.createSupplier(MptSupplierAssembler.INSTANCE.toDto(supplier), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 修改保存供应商信息
     *
     * @param supplier 供应商信息
     * @return 结果
     */
    @Log(title = "供应商管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:vehicle:supplier:edit")
    @PutMapping
    public ApiResponse<Void> edit(@Validated @RequestBody SupplierVo supplier) {
        log.info("管理后台用户[{}]修改保存供应商信息[{}]", SecurityUtils.getUsername(), supplier.getCode());
        if (!supplierAppService.checkCodeUnique(supplier.getId(), supplier.getCode())) {
            return ApiResponse.fail("修改保存供应商'" + supplier.getCode() + "'失败，供应商代码已存在");
        }
        supplierAppService.modifySupplier(MptSupplierAssembler.INSTANCE.toDto(supplier), SecurityUtils.getUserId().toString());
        return ApiResponse.ok();
    }

    /**
     * 删除供应商信息
     *
     * @param supplierIds 供应商ID数组
     * @return 结果
     */
    @Log(title = "供应商管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:vehicle:supplier:remove")
    @DeleteMapping("/{supplierIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] supplierIds) {
        log.info("管理后台用户[{}]删除供应商信息[{}]", SecurityUtils.getUsername(), supplierIds);
        return supplierAppService.deleteSupplierByIds(supplierIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

}
