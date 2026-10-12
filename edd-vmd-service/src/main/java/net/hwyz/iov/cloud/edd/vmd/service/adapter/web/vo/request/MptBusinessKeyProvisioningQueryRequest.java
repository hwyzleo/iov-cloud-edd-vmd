package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MPT 业务密钥开通批次查询请求（CR-058）。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptBusinessKeyProvisioningQueryRequest {

    /** TBOX 稳定 requestId */
    @NotBlank(message = "requestId 不能为空")
    private String requestId;
}
