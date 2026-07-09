package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 防盗根下发请求
 *
 * @author hwyz_leo
 * @since 2026-07-08
 */
@Data
public class ImmoKeyDeriveRequest {

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
