package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务密钥管理查询请求（CR-055 §7.3）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptBusinessKeyQueryRequest {

    /**
     * 设备实例序列号
     */
    @NotBlank(message = "deviceSn 不能为空")
    private String deviceSn;

    /**
     * 受治理业务域代码（可空，空为全部域）
     */
    private String businessDomain;

    /**
     * 受治理用途代码（可空，空为全部用途）
     */
    private String purpose;
}
