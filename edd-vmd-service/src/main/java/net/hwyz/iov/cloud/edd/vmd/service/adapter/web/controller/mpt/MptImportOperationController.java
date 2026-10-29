package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ImportOperationAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperationItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfoChangeLog;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleInfoChangeLog;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoChangeLogRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleInfoChangeLogRepository;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统一导入操作管理接口（CR-071 §5.1.24）
 * <p>
 * REPLAY（补发）与 REIMPORT（重导）统一门面：preview → execute → retry → 查询；
 * 以及零件/车辆属性历史查询。仅后台能力，不扩展服务端 Feign 写入口。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/importOperation/v1")
public class MptImportOperationController {

    private final ImportOperationAppService importOperationAppService;
    private final PartInfoChangeLogRepository partInfoChangeLogRepository;
    private final VehicleInfoChangeLogRepository vehicleInfoChangeLogRepository;

    /**
     * 预检：候选识别 + 计划摘要
     */
    @RequiresPermissions("vmd:importOperation:preview")
    @PostMapping("/preview")
    public ApiResponse<ImportOperationAppService.PreviewResult> preview(@RequestBody Map<String, Object> request) {
        String domain = (String) request.get("domain");
        Long importDataId = ((Number) request.get("importDataId")).longValue();
        String mode = (String) request.get("mode");
        @SuppressWarnings("unchecked")
        List<String> scope = (List<String>) request.get("scope");
        String repairPolicy = (String) request.get("repairPolicy");
        log.info("管理后台用户[{}]导入操作预检: domain={}, importDataId={}, mode={}",
                SecurityContextHolder.getUserName(), domain, importDataId, mode);
        ImportOperationAppService.PreviewResult result = importOperationAppService.preview(
                new ImportOperationAppService.PreviewRequest(domain, importDataId, mode, scope, repairPolicy));
        return ApiResponse.ok(result);
    }

    /**
     * 执行（REIMPORT/REPLAY）
     */
    @RequiresPermissions("vmd:importOperation:execute")
    @PostMapping("/execute")
    public ApiResponse<ImportOperationAppService.ExecuteResult> execute(@RequestBody Map<String, Object> request) {
        String domain = (String) request.get("domain");
        Long importDataId = ((Number) request.get("importDataId")).longValue();
        String mode = (String) request.get("mode");
        @SuppressWarnings("unchecked")
        List<String> scope = (List<String>) request.get("scope");
        String repairPolicy = (String) request.get("repairPolicy");
        String planDigest = (String) request.get("planDigest");
        String reason = (String) request.get("reason");
        String operationId = (String) request.get("operationId");
        String userId = SecurityUtils.getUserId() != null ? SecurityUtils.getUserId().toString() : "SYSTEM";
        log.info("管理后台用户[{}]导入操作执行: domain={}, importDataId={}, mode={}, reason={}",
                SecurityContextHolder.getUserName(), domain, importDataId, mode, reason);
        ImportOperationAppService.ExecuteResult result = importOperationAppService.execute(
                ImportOperationAppService.ExecuteRequest.builder()
                        .domain(domain)
                        .importDataId(importDataId)
                        .mode(mode)
                        .scope(scope)
                        .repairPolicy(repairPolicy)
                        .planDigest(planDigest)
                        .reason(reason)
                        .operationId(operationId)
                        .operatorId(userId)
                        .operatorName(SecurityContextHolder.getUserName())
                        .build());
        return ApiResponse.ok(result);
    }

    /**
     * 重试
     */
    @RequiresPermissions("vmd:importOperation:execute")
    @PostMapping("/{operationId}/retry")
    public ApiResponse<ImportOperationAppService.ExecuteResult> retry(@PathVariable String operationId) {
        String userId = SecurityUtils.getUserId() != null ? SecurityUtils.getUserId().toString() : "SYSTEM";
        return ApiResponse.ok(importOperationAppService.retry(operationId, userId));
    }

    /**
     * 查询操作
     */
    @RequiresPermissions("vmd:importOperation:query")
    @GetMapping("/{operationId}")
    public ApiResponse<ImportOperation> query(@PathVariable String operationId) {
        return ApiResponse.ok(importOperationAppService.queryOperation(operationId));
    }

    /**
     * 查询操作明细（游标分页）
     */
    @RequiresPermissions("vmd:importOperation:query")
    @GetMapping("/{operationId}/items")
    public ApiResponse<List<ImportOperationItem>> items(@PathVariable String operationId,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) Long cursor,
                                                        @RequestParam(defaultValue = "100") Integer limit) {
        return ApiResponse.ok(importOperationAppService.queryItems(operationId, status, cursor, limit));
    }

    /**
     * 查询零件实例变更历史
     */
    @RequiresPermissions("vmd:partInfo:history:query")
    @GetMapping("/part/{partId}/changes")
    public ApiResponse<List<PartInfoChangeLog>> partChanges(@PathVariable Long partId) {
        return ApiResponse.ok(partInfoChangeLogRepository.selectByPartId(partId));
    }

    /**
     * 查询车辆变更历史
     */
    @RequiresPermissions("vmd:vehicle:history:query")
    @GetMapping("/vehicle/{vin}/changes")
    public ApiResponse<List<VehicleInfoChangeLog>> vehicleChanges(@PathVariable String vin) {
        return ApiResponse.ok(vehicleInfoChangeLogRepository.selectByVin(vin));
    }

}
