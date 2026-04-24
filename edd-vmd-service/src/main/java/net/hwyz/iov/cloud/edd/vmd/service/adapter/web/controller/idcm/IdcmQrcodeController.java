package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.idcm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.QrcodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.QrcodeAppService;
import org.springframework.web.bind.annotation.*;

/**
 * 二维码相关车机接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/idcm/qrcode/v1")
public class IdcmQrcodeController {

    private final QrcodeAppService qrcodeAppService;

    /**
     * 生成车辆激活二维码
     *
     * @param vin      车架号
     * @param clientId 客户端ID
     * @return 二维码返回
     */
    @PostMapping("/action/generateActiveQrcode")
    public ApiResponse<QrcodeResponse> generateActiveQrcode(@RequestHeader String vin, @RequestHeader String clientId) {
        log.info("车辆 [{}] 车机 [{}] 生成车辆激活二维码", vin, clientId);
        return ApiResponse.ok(qrcodeAppService.generateActiveQrcode(vin, clientId));
    }

    /**
     * 获取车辆激活二维码状态
     *
     * @param vin      车架号
     * @param clientId 客户端ID
     * @return 二维码返回
     */
    @GetMapping("/active/{qrcode}")
    public ApiResponse<QrcodeResponse> getActiveQrcodeState(@PathVariable("qrcode") String qrcode, @RequestHeader String vin,
                                                            @RequestHeader String clientId) {
        log.info("车辆 [{}] 车机 [{}] 获取车辆激活二维码状态", vin, clientId);
        return ApiResponse.ok(qrcodeAppService.getActiveQrcodeState(qrcode, vin, clientId));
    }
}
