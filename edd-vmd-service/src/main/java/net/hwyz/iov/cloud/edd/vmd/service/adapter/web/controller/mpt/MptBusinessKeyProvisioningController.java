package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.MptBusinessKeyClearBlockingRequest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.OperatorContext;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.TerminateStaleProvisioningCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateProvisioningResolutionResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ProvisioningBlockingDiagnosisResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ProvisioningTerminationResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyProvisioningMaintenanceAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.Kl1LegacyMigrationService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.Kl1LegacyMigrationService.MigrationReport;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.TerminationReasonCode;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.audit.enums.OperatorType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务密钥开通批次——运维“一键清理阻塞”接口（VMD-DSN-CR-065 §6）。
 * <p>
 * 后台功能面向“直接解除旧批次对新证书流程的占用”，不要求管理员理解领域状态机：
 * <ul>
 *   <li>GET /api/mpt/business-key/provisioning/by-certificate-request/{certificateRequestId}（证书维度寻址，只读桥接）；</li>
 *   <li>GET /api/mpt/business-key/provisioning/{requestId}/blocking-diagnosis（只读诊断）；</li>
 *   <li>POST /api/mpt/business-key/provisioning/{requestId}/clear-blocking（唯一破坏性入口）。</li>
 * </ul>
 * 证书申请 requestId 与 Keyprov 批次 requestId 是两套独立标识，本控制器不得混用、猜测或让
 * 诊断/清理接口兼容解释两种 ID（VMD-DSN-CR-065 §6）。
 * 服务端单次完成执行时复核、逻辑终止、PENDING 连带失败化与审计；不得签发/升级/撤销证书，
 * 不得代替车端创建新批次。生产建议仅授予安全运维角色；不得暴露给 VAGW/TBOX 或普通业务服务。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/business-key/provisioning")
public class MptBusinessKeyProvisioningController extends BaseController {

    private final BusinessKeyProvisioningMaintenanceAppService maintenanceAppService;
    private final Kl1LegacyMigrationService kl1LegacyMigrationService;

    /**
     * 证书维度寻址开通批次（只读桥接，VMD-DSN-CR-065 §6.2，方案 A）。
     * <p>
     * 从证书申请 requestId（MPT-CERT-* 等）解析同设备+业务域唯一未终结 Keyprov 批次 requestId（kp-*），
     * 前端取真实批次号后再调用 {@link #blockingDiagnosis}/{@link #clearBlocking}；
     * 诊断/清理接口不接受证书申请 requestId，两套 ID 不得混用。
     *
     * @param certificateRequestId 证书申请 requestId
     * @param businessDomain       业务域（缺省 V2C_COMM）
     * @param includeTerminal      是否附带历史终结批次（仅供审计）
     */
    @RequiresPermissions("vmd:security:businessKey:provisioning:query")
    @GetMapping("/by-certificate-request/{certificateRequestId}")
    public ApiResponse<CertificateProvisioningResolutionResult> resolveByCertificateRequest(
            @PathVariable String certificateRequestId,
            @RequestParam(value = "businessDomain", defaultValue = "V2C_COMM") String businessDomain,
            @RequestParam(value = "includeTerminal", defaultValue = "false") boolean includeTerminal) {
        log.info("MPT请求按证书申请requestId寻址开通批次: certificateRequestId={}, businessDomain={}, includeTerminal={}",
                certificateRequestId, businessDomain, includeTerminal);
        return ApiResponse.ok(maintenanceAppService.resolveByCertificateRequest(
                certificateRequestId, businessDomain, includeTerminal));
    }

    /**
     * KL1 存量迁移 dry-run（只读计数与影子读取差异，不落库；VMD-DSN-CR-069 §5.2）
     */
    @RequiresPermissions("vmd:security:businessKey:provisioning:query")
    @GetMapping("/kl1-migration/dry-run")
    public ApiResponse<MigrationReport> kl1MigrationDryRun(
            @RequestParam(value = "limit", defaultValue = "200") int limit) {
        log.info("MPT请求KL1存量迁移dry-run: limit={}", limit);
        return ApiResponse.ok(kl1LegacyMigrationService.dryRun(limit));
    }

    /**
     * KL1 存量迁移（幂等回填/受控终结/quarantine；重跑不重置 F/B/lease_seq；VMD-DSN-CR-069 §5）
     */
    @Log(title = "KL1存量批次迁移", businessType = BusinessType.UPDATE, operatorType = OperatorType.MANAGE,
            isSaveRequestData = true, isSaveResponseData = true)
    @RequiresPermissions("vmd:security:businessKey:provisioning:terminate")
    @PostMapping("/kl1-migration")
    public ApiResponse<MigrationReport> kl1Migration(
            @RequestParam(value = "limit", defaultValue = "200") int limit) {
        log.info("MPT请求KL1存量迁移: limit={}", limit);
        return ApiResponse.ok(kl1LegacyMigrationService.migrate(limit));
    }

    /**
     * 阻塞诊断（只读，供页面展示）
     */
    @RequiresPermissions("vmd:security:businessKey:provisioning:query")
    @GetMapping("/{requestId}/blocking-diagnosis")
    public ApiResponse<ProvisioningBlockingDiagnosisResult> blockingDiagnosis(@PathVariable String requestId) {
        log.info("MPT请求业务密钥开通批次阻塞诊断: requestId={}", requestId);
        return ApiResponse.ok(maintenanceAppService.inspectTermination(requestId));
    }

    /**
     * 一键清理阻塞（唯一破坏性入口；执行时服务端重做全部门禁）
     */
    @Log(title = "业务密钥开通批次清理阻塞", businessType = BusinessType.UPDATE, operatorType = OperatorType.MANAGE,
            isSaveRequestData = true, isSaveResponseData = true)
    @RequiresPermissions("vmd:security:businessKey:provisioning:terminate")
    @PostMapping("/{requestId}/clear-blocking")
    public ApiResponse<ProvisioningTerminationResult> clearBlocking(
            @PathVariable String requestId,
            @RequestBody @Validated MptBusinessKeyClearBlockingRequest request,
            HttpServletRequest httpRequest) {
        log.info("MPT请求业务密钥开通批次清理阻塞: requestId={}, maintenanceRequestId={}, reasonCode={}, ticketNo={}",
                requestId, request.getMaintenanceRequestId(), request.getReasonCode(), request.getTicketNo());
        TerminateStaleProvisioningCommand cmd = TerminateStaleProvisioningCommand.builder()
                .maintenanceRequestId(request.getMaintenanceRequestId())
                .requestId(requestId)
                .expectedState(BusinessKeyProvisioningState.valOf(request.getExpectedState()))
                .expectedModifyTime(request.getExpectedModifyTime())
                .reasonCode(TerminationReasonCode.valOf(request.getReasonCode()))
                .reason(request.getReason())
                .ticketNo(request.getTicketNo())
                .build();
        OperatorContext operatorCtx = OperatorContext.builder()
                .operatorId(safeUserId())
                .operatorName(safeUserName())
                .sourceIp(httpRequest != null ? httpRequest.getRemoteAddr() : null)
                .userAgent(httpRequest != null ? httpRequest.getHeader("User-Agent") : null)
                .build();
        return ApiResponse.ok(maintenanceAppService.terminateStale(cmd, operatorCtx));
    }

    private String safeUserId() {
        try {
            Long userId = SecurityUtils.getUserId();
            return userId != null ? String.valueOf(userId) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String safeUserName() {
        try {
            return net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder.getUserName();
        } catch (Exception e) {
            return null;
        }
    }
}
