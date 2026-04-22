package net.hwyz.iov.cloud.edd.vmd.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.QrcodeState;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.QrcodeType;

import java.util.Map;

/**
 * 二维码响应
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrcodeResponse {

    /**
     * 二维码
     */
    private String qrcode;
    /**
     * 二维码类型
     */
    private QrcodeType type;
    /**
     * 二维码状态
     */
    private QrcodeState state;
    /**
     * 额外信息
     */
    private Map<String, Object> extras;

}
