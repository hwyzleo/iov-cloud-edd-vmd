package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.idcm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.QrcodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.WebQrcodeAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.QrcodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.QrcodeAppService;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.web.bind.annotation.*;

/**
 * 车机端二维码相关接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/idcm/qrcode/v1")
public class IdcmQrcodeController extends BaseController {

    private final QrcodeAppService qrcodeAppService;

    /**
     * 生成车辆激活二维码
     *
     * @param vin 车架号
     * @param sn  车机序列号
     * @return 二维码状态
     */
    @PostMapping("/active")
    public ApiResponse<QrcodeResponse> generateActiveQrcode(@RequestHeader("X-VIN") String vin,
                                                           @RequestHeader("X-SN") String sn) {
        log.info("车机端请求生成车辆[{}]激活二维码", vin);
        QrcodeDto qrcodeDto = qrcodeAppService.generateActiveQrcode(vin, sn);
        return ApiResponse.ok(WebQrcodeAssembler.INSTANCE.fromDto(qrcodeDto));
    }

    /**
     * 获取车辆激活二维码状态
     *
     * @param qrcode 二维码
     * @param vin    车架号
     * @param sn     车机序列号
     * @return 二维码状态
     */
    @GetMapping("/active/{qrcode}/state")
    public ApiResponse<QrcodeResponse> getActiveQrcodeState(@PathVariable String qrcode,
                                                           @RequestHeader("X-VIN") String vin,
                                                           @RequestHeader("X-SN") String sn) {
        log.info("车机端请求获取车辆[{}]激活二维码[{}]状态", vin, qrcode);
        QrcodeDto qrcodeDto = qrcodeAppService.getActiveQrcodeState(qrcode, vin, sn);
        return ApiResponse.ok(WebQrcodeAssembler.INSTANCE.fromDto(qrcodeDto));
    }

}
