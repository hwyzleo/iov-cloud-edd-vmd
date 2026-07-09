package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * OTA根下发请求
 *
 * @author hwyz_leo
 * @since 2026-07-09
 */
@Data
public class OtaRootDeriveRequest {

    /**
     * 车架号
     */
    @NotBlank(message = "车架号不能为空")
    private String vin;

    /**
     * 灌注机唯一标识
     */
    @NotBlank(message = "灌注机唯一标识不能为空")
    private String facilityUid;
}
