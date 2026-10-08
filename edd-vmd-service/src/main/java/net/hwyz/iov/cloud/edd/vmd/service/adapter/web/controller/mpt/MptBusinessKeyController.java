package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.MptBusinessKeyQueryRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.MptBusinessKeyRevokeRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.MptBusinessKeyRotateRequest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BusinessKeyRevokeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BusinessKeyRotateCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyMetadataResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyRevokeResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyRotationResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyDirectoryAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyRevocationAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyRotationAppService;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 业务密钥管理接口（CR-055 §7.3）
 * <p>
 * 首期提供 query/rotate/revoke，权限 vmd:security:businessKey:query/rotate/revoke。
 * 批量轮换、审批和动态策略后台不在本 CR 范围。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/businessKey/v1")
public class MptBusinessKeyController extends BaseController {

    private final BusinessKeyDirectoryAppService businessKeyDirectoryAppService;
    private final BusinessKeyRotationAppService businessKeyRotationAppService;
    private final BusinessKeyRevocationAppService businessKeyRevocationAppService;

    /**
     * 查询设备业务密钥（按设备，可限定域/用途）
     *
     * @param request 查询请求
     * @return 业务密钥元数据列表
     */
    @RequiresPermissions("vmd:security:businessKey:query")
    @PostMapping("/query")
    public ApiResponse<List<BusinessKeyMetadataResult>> query(@RequestBody @Validated MptBusinessKeyQueryRequest request) {
        log.info("MPT请求查询业务密钥: deviceSn={}, domain={}, purpose={}",
                request.getDeviceSn(), request.getBusinessDomain(), request.getPurpose());
        List<BusinessKeyMetadataResult> result = businessKeyDirectoryAppService.queryByDevice(
                request.getDeviceSn(), request.getBusinessDomain(), request.getPurpose());
        return ApiResponse.ok(result);
    }

    /**
     * 轮换业务密钥（创建新材料并原子切换 ACTIVE）
     *
     * @param request 轮换请求
     * @return 轮换结果
     */
    @RequiresPermissions("vmd:security:businessKey:rotate")
    @PostMapping("/rotate")
    public ApiResponse<BusinessKeyRotationResult> rotate(@RequestBody @Validated MptBusinessKeyRotateRequest request) {
        log.info("MPT请求轮换业务密钥: requestId={}, deviceSn={}, domain={}, purpose={}",
                request.getRequestId(), request.getDeviceSn(), request.getBusinessDomain(), request.getPurpose());
        BusinessKeyRotateCmd cmd = BusinessKeyRotateCmd.builder()
                .requestId(request.getRequestId())
                .deviceSn(request.getDeviceSn())
                .deviceCategory(request.getDeviceCategory())
                .businessDomain(request.getBusinessDomain())
                .purpose(request.getPurpose())
                .operatorId(safeUserId())
                .build();
        return ApiResponse.ok(businessKeyRotationAppService.rotate(cmd));
    }

    /**
     * 吊销业务密钥（先阻断再吊销，结果未知需对账）
     *
     * @param request 吊销请求
     * @return 吊销结果
     */
    @RequiresPermissions("vmd:security:businessKey:revoke")
    @PostMapping("/revoke")
    public ApiResponse<BusinessKeyRevokeResult> revoke(@RequestBody @Validated MptBusinessKeyRevokeRequest request) {
        log.info("MPT请求吊销业务密钥: requestId={}, keyId={}, reason={}",
                request.getRequestId(), request.getKeyId(), request.getReason());
        BusinessKeyRevokeCmd cmd = BusinessKeyRevokeCmd.builder()
                .requestId(request.getRequestId())
                .keyId(request.getKeyId())
                .reason(request.getReason())
                .operatorId(safeUserId())
                .build();
        return ApiResponse.ok(businessKeyRevocationAppService.revoke(cmd));
    }

    private String safeUserId() {
        try {
            Long userId = SecurityUtils.getUserId();
            return userId != null ? String.valueOf(userId) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
