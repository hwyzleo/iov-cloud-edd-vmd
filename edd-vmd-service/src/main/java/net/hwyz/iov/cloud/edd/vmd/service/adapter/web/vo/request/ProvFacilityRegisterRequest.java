package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 安全灌注机注册请求
 *
 * @author hwyz_leo
 * @since 2026-07-08
 */
@Data
public class ProvFacilityRegisterRequest {

    /**
     * 灌注机唯一标识
     */
    @NotBlank(message = "灌注机唯一标识不能为空")
    private String facilityUid;
}
