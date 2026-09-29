package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptVehicleCertificateAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.CertificateConfirmInstalledRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.CertificateReconcileRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.CompensateCertificateRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.VehicleCertificateQueryRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.CertificateCompensateResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.CertificateDetailResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.CertificateListResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CompensateCertificateCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehicleCertificateQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateCompensateResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateDetailResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateListResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.CertificateCompensationAppService;
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
 * 车辆设备证书管理后台补偿接口（MPT，CR-053 / US-060）
 * <p>
 * 提供申请记录查询、人工补申请、已有申请继续/对账及安装结果补录。
 * 所有写动作复用 {@link net.hwyz.iov.cloud.edd.vmd.service.application.service.CertificateProvisioningAppService}，
 * 不复制 PKI 调用逻辑；后台不生成设备密钥或 CSR。
 *
 * @author hwyz_leo
 * @since 2026-09-29
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/vehicleCertificate/v1")
public class MptVehicleCertificateController extends BaseController {

    private final CertificateCompensationAppService certificateCompensationAppService;

    /**
     * 分页查询证书申请记录
     * <p>
     * 列表不返回 CSR 全文（CSR 全文本就不落库），仅返回证书元数据与状态。
     *
     * @param request 查询请求
     * @return 分页列表
     */
    @RequiresPermissions("vmd:security:vehicleCertificate:list")
    @GetMapping("/list")
    public ApiResponse<PageResult<CertificateListResponse>> list(VehicleCertificateQueryRequest request) {
        log.info("管理后台用户[{}]分页查询证书申请记录", SecurityContextHolder.getUserName());
        startPage();
        VehicleCertificateQuery query = MptVehicleCertificateAssembler.INSTANCE.toQuery(request);
        query.setBeginTime(toLocalDateTime(getBeginTime(request)));
        query.setEndTime(toLocalDateTime(getEndTime(request)));
        List<CertificateListResult> results = certificateCompensationAppService.search(query);
        return ApiResponse.ok(getPageResult(PageUtil.convert(results, MptVehicleCertificateAssembler.INSTANCE::toListResponse)));
    }

    /**
     * 查询证书申请详情与脱敏操作审计时间线
     *
     * @param id 证书记录主键
     * @return 详情
     */
    @RequiresPermissions("vmd:security:vehicleCertificate:query")
    @GetMapping("/{id}")
    public ApiResponse<CertificateDetailResponse> getDetail(@PathVariable Long id) {
        log.info("管理后台用户[{}]查询证书申请详情[id={}]", SecurityContextHolder.getUserName(), id);
        CertificateDetailResult detail = certificateCompensationAppService.getDetail(id);
        CertificateDetailResponse response = MptVehicleCertificateAssembler.INSTANCE.toDetailResponse(detail);
        response.setOperations(MptVehicleCertificateAssembler.INSTANCE.toOperationResponseList(detail.getOperations()));
        return ApiResponse.ok(response);
    }

    /**
     * 人工补申请（MES 请求未达 VMD）
     *
     * @param request   补偿请求
     * @param servletRequest HTTP请求（来源IP/UA审计）
     * @return 补偿结果
     */
    @Log(title = "证书人工补申请", businessType = BusinessType.INSERT)
    @RequiresPermissions("vmd:security:vehicleCertificate:compensate")
    @PostMapping("/compensate")
    public ApiResponse<CertificateCompensateResponse> compensate(@RequestBody @Validated CompensateCertificateRequest request,
                                                                 HttpServletRequest servletRequest) {
        log.info("管理后台用户[{}]人工补申请证书, vin={}, deviceSn={}",
                SecurityContextHolder.getUserName(), request.getVin(), request.getDeviceSn());
        CompensateCertificateCmd cmd = MptVehicleCertificateAssembler.INSTANCE.toCmd(request);
        cmd.setOperatorId(SecurityUtils.getUserId() != null ? SecurityUtils.getUserId().toString() : null);
        cmd.setOperatorName(SecurityContextHolder.getUserName());
        cmd.setSourceIp(getRemoteIp(servletRequest));
        cmd.setUserAgent(servletRequest.getHeader("User-Agent"));
        CertificateCompensateResult result = certificateCompensationAppService.compensate(cmd);
        return ApiResponse.ok(MptVehicleCertificateAssembler.INSTANCE.toCompensateResponse(result));
    }

    /**
     * 对已有申请继续/对账
     *
     * @param id            证书记录主键
     * @param request       对账请求（reason 可空）
     * @param servletRequest HTTP请求（来源IP/UA审计）
     * @return 对账结果
     */
    @Log(title = "证书申请对账", businessType = BusinessType.OTHER)
    @RequiresPermissions("vmd:security:vehicleCertificate:compensate")
    @PostMapping("/{id}/reconcile")
    public ApiResponse<CertificateCompensateResponse> reconcile(@PathVariable Long id,
                                                                @RequestBody(required = false) CertificateReconcileRequest request,
                                                                HttpServletRequest servletRequest) {
        log.info("管理后台用户[{}]对账证书申请[id={}]", SecurityContextHolder.getUserName(), id);
        String reason = request != null ? request.getReason() : null;
        CertificateCompensateResult result = certificateCompensationAppService.reconcile(
                id,
                reason,
                SecurityUtils.getUserId() != null ? SecurityUtils.getUserId().toString() : null,
                SecurityContextHolder.getUserName(),
                getRemoteIp(servletRequest),
                servletRequest.getHeader("User-Agent")
        );
        return ApiResponse.ok(MptVehicleCertificateAssembler.INSTANCE.toCompensateResponse(result));
    }

    /**
     * 安装结果补录（受控，原因与工单必填）
     *
     * @param id            证书记录主键
     * @param request       补录请求
     * @param servletRequest HTTP请求（来源IP/UA审计）
     * @return 补录结果
     */
    @Log(title = "证书安装结果补录", businessType = BusinessType.UPDATE)
    @RequiresPermissions("vmd:security:vehicleCertificate:confirm")
    @PostMapping("/{id}/confirmInstalled")
    public ApiResponse<CertificateCompensateResponse> confirmInstalled(@PathVariable Long id,
                                                                       @RequestBody @Validated CertificateConfirmInstalledRequest request,
                                                                       HttpServletRequest servletRequest) {
        log.info("管理后台用户[{}]补录证书安装结果[id={}], result={}",
                SecurityContextHolder.getUserName(), id, request.getResult());
        CertificateCompensateResult result = certificateCompensationAppService.confirmInstalled(
                id,
                request.getCertSn(),
                request.getDeviceSn(),
                request.getResult(),
                request.getFailReason(),
                request.getReason(),
                request.getTicketNo(),
                SecurityUtils.getUserId() != null ? SecurityUtils.getUserId().toString() : null,
                SecurityContextHolder.getUserName(),
                getRemoteIp(servletRequest),
                servletRequest.getHeader("User-Agent")
        );
        return ApiResponse.ok(MptVehicleCertificateAssembler.INSTANCE.toCompensateResponse(result));
    }

    /**
     * 获取客户端IP（带代理头回退）
     */
    private String getRemoteIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }

    /**
     * java.util.Date 转 LocalDateTime（null 安全）
     */
    private java.time.LocalDateTime toLocalDateTime(java.util.Date date) {
        return date != null ? java.time.LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault()) : null;
    }

}
