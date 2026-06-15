package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.PartImportDataRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ImportResultResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.PartImportDataResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptPartImportDataAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PartImportDataCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartImportDataDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.PartImportDataQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartImportDataAppService;
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
 * 零件导入数据相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/partImportData/v1")
public class MptPartImportDataController extends BaseController {

    private final PartImportDataAppService partImportDataAppService;

    @RequiresPermissions("completeVehicle:vehicle:importData:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<PartImportDataResponse>> list(PartImportDataRequest partImportData) {
        log.info("管理后台用户[{}]分页查询零件导入数据", SecurityContextHolder.getUserName());
        startPage();
        PartImportDataQuery query = PartImportDataQuery.builder()
                .batchNum(partImportData.getBatchNum())
                .partCode(partImportData.getPartCode())
                .handle(partImportData.getHandle())
                .beginTime(getBeginTime(partImportData))
                .endTime(getEndTime(partImportData))
                .build();
        List<PartImportDataDto> dtoList = partImportDataAppService.search(query);
        return ApiResponse.ok(getPageResult(PageUtil.convert(dtoList, MptPartImportDataAssembler.INSTANCE::fromDto)));
    }

    @Log(title = "零件导入数据管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:vehicle:importData:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, PartImportDataRequest partImportData) {
        log.info("管理后台用户[{}]导出零件导入数据", SecurityContextHolder.getUserName());
    }

    @RequiresPermissions("completeVehicle:vehicle:importData:query")
    @GetMapping(value = "/{partImportDataId}")
    public ApiResponse<PartImportDataResponse> getInfo(@PathVariable Long partImportDataId) {
        log.info("管理后台用户[{}]根据零件导入数据ID[{}]获取零件导入数据", SecurityContextHolder.getUserName(), partImportDataId);
        return ApiResponse.ok(MptPartImportDataAssembler.INSTANCE.fromDto(partImportDataAppService.getPartImportDataById(partImportDataId)));
    }

    @Log(title = "零件导入数据管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:vehicle:importData:add")
    @PostMapping
    public ApiResponse<ImportResultResponse> add(@Validated @RequestBody PartImportDataRequest partImportData) {
        log.info("管理后台用户[{}]新增零件导入数据[{}]", SecurityContextHolder.getUserName(), partImportData.getBatchNum());
        if (!partImportDataAppService.checkBatchNumUnique(partImportData.getId(), partImportData.getBatchNum())) {
            return ApiResponse.fail("新增零件导入数据'" + partImportData.getBatchNum() + "'失败，批次号已存在");
        }
        PartImportDataCmd cmd = MptPartImportDataAssembler.INSTANCE.toCmd(partImportData);
        if (partImportDataAppService.createPartImportData(cmd, SecurityUtils.getUserId().toString()) <= 0) {
            return ApiResponse.fail("操作失败");
        }
        try {
            ImportResult result = partImportDataAppService.parsePartImportData(partImportData.getBatchNum());
            ImportResultResponse response = ImportResultResponse.builder()
                    .totalCount(result.getTotalCount())
                    .successCount(result.getSuccessCount())
                    .failureCount(result.getFailureCount())
                    .invalidCount(result.getInvalidCount())
                    .build();
            return ApiResponse.ok(response);
        } catch (Exception e) {
            log.error("零件导入数据[{}]解析异常", partImportData.getBatchNum(), e);
            return ApiResponse.fail("零件导入数据'" + partImportData.getBatchNum() + "'解析异常");
        }
    }

    @Log(title = "零件导入数据管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:vehicle:importData:edit")
    @PutMapping
    public ApiResponse<ImportResultResponse> edit(@Validated @RequestBody PartImportDataRequest partImportData) {
        log.info("管理后台用户[{}]修改保存零件导入数据[{}]", SecurityContextHolder.getUserName(), partImportData.getBatchNum());
        if (!partImportDataAppService.checkBatchNumUnique(partImportData.getId(), partImportData.getBatchNum())) {
            return ApiResponse.fail("修改保存零件导入数据'" + partImportData.getBatchNum() + "'失败，批次号已存在");
        }
        PartImportDataCmd cmd = MptPartImportDataAssembler.INSTANCE.toCmd(partImportData);
        if (partImportDataAppService.modifyPartImportData(cmd, SecurityUtils.getUserId().toString()) <= 0) {
            return ApiResponse.fail("操作失败");
        }
        try {
            ImportResult result = partImportDataAppService.parsePartImportData(partImportData.getBatchNum());
            ImportResultResponse response = ImportResultResponse.builder()
                    .totalCount(result.getTotalCount())
                    .successCount(result.getSuccessCount())
                    .failureCount(result.getFailureCount())
                    .invalidCount(result.getInvalidCount())
                    .build();
            return ApiResponse.ok(response);
        } catch (Exception e) {
            log.error("零件导入数据[{}]解析异常", partImportData.getBatchNum(), e);
            return ApiResponse.fail("零件导入数据'" + partImportData.getBatchNum() + "'解析异常");
        }
    }

    @Log(title = "零件导入数据管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:vehicle:importData:remove")
    @DeleteMapping("/{partImportDataIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] partImportDataIds) {
        log.info("管理后台用户[{}]删除零件导入数据[{}]", SecurityContextHolder.getUserName(), partImportDataIds);
        return partImportDataAppService.deletePartImportDataByIds(partImportDataIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }
}
