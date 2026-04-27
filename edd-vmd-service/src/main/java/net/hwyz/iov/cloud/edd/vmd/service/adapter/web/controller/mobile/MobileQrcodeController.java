package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mobile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.request.QrcodeRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.QrcodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.WebQrcodeAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.QrcodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.QrcodeAppService;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 移动端二维码相关接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mobile/qrcode/v1")
public class MobileQrcodeController extends BaseController {

    private final QrcodeAppService qrcodeAppService;

    /**
     * 扫描验证二维码
     *
     * @param qrcodeRequest 二维码请求
     * @return 二维码状态
     */
    @PostMapping("/validate")
    public ApiResponse<QrcodeResponse> validateQrcode(@Validated @RequestBody QrcodeRequest qrcodeRequest) {
        log.info("移动端用户[{}]请求扫描验证二维码[{}]", SecurityUtils.getUserId(), qrcodeRequest.getQrcode());
        QrcodeDto qrcodeDto = qrcodeAppService.validateQrcode(qrcodeRequest.getQrcode(), SecurityUtils.getUserId().toString());
        return ApiResponse.ok(WebQrcodeAssembler.INSTANCE.fromDto(qrcodeDto));
    }

    /**
     * 确认二维码
     *
     * @param qrcodeRequest 二维码请求
     * @return 二维码状态
     */
    @PostMapping("/confirm")
    public ApiResponse<QrcodeResponse> confirmQrcode(@Validated @RequestBody QrcodeRequest qrcodeRequest) {
        log.info("移动端用户[{}]请求确认二维码[{}]", SecurityUtils.getUserId(), qrcodeRequest.getQrcode());
        QrcodeDto qrcodeDto = qrcodeAppService.confirmQrcode(qrcodeRequest.getQrcode(), SecurityUtils.getUserId().toString());
        return ApiResponse.ok(WebQrcodeAssembler.INSTANCE.fromDto(qrcodeDto));
    }

}
