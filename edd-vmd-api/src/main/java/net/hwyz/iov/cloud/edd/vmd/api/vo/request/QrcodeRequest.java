package net.hwyz.iov.cloud.edd.vmd.api.vo.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 二维码请求
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrcodeRequest {

    /**
     * 二维码
     */
    @NotEmpty(message = "二维码不允许为空")
    private String qrcode;

}
