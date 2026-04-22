package net.hwyz.iov.cloud.edd.vmd.service.controller.mobile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ClientAccount;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.edd.vmd.api.vo.request.QrcodeRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.QrcodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.QrcodeAppService;
import org.springframework.web.bind.annotation.*;

/**
 * 二维码相关手机接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mobile/qrcode/v1")
public class MobileQrcodeController {

    private final QrcodeAppService qrcodeAppService;

    /**
     * 扫描验证二维码
     *
     * @param request       二维码请求
     * @param clientAccount 终端用户
     * @return 验证结果
     */
    @PostMapping("/action/validateQrcode")
    public ApiResponse<QrcodeResponse> validateQrcode(@RequestBody @Valid QrcodeRequest request,
                                                      @RequestHeader ClientAccount clientAccount) {
        log.info("手机客户端 [{}] 验证车辆二维码", ParamHelper.getClientAccountInfo(clientAccount));
        return ApiResponse.ok(qrcodeAppService.validateQrcode(request.getQrcode(), clientAccount.getAccountId()));
    }

    /**
     * 确认二维码
     *
     * @param request       二维码请求
     * @param clientAccount 终端用户
     * @return 确认结果
     */
    @PostMapping("/action/confirmQrcode")
    public ApiResponse<QrcodeResponse> confirmQrcode(@RequestBody @Valid QrcodeRequest request,
                                                     @RequestHeader ClientAccount clientAccount) {
        log.info("手机客户端 [{}] 确认车辆二维码", ParamHelper.getClientAccountInfo(clientAccount));
        return ApiResponse.ok(qrcodeAppService.confirmQrcode(request.getQrcode(), clientAccount.getAccountId()));
    }

}
