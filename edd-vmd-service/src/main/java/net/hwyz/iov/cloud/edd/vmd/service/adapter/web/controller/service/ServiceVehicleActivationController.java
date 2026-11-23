package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.VehicleActivationState;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehicleActivationNodeExResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleActivationDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleActivationQueryAppService;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.exception.InputParamInvalidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车辆首次激活事实只读服务接口实现（VMD-DSN-CR-077 §7）
 * <p>
 * 只读契约：不写投影/台账/生命周期/outbox、不发 Kafka、不调 Feign（D-VMD-077-10）。
 * 三态以 200 + {@code state} 表达，不使用 404 表达「无事实」；
 * 技术失败 fail-closed 抛 806137（ACTIVATION_FACT_QUERY_UNAVAILABLE），绝不返回 NOT_RECORDED。
 * <p>
 * 日志脱敏：VIN 前 3 后 4；不输出 sourceFactRef 原文。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/vehicleActivation/v1")
public class ServiceVehicleActivationController extends BaseController {

    private final VehicleActivationQueryAppService vehicleActivationQueryAppService;

    /**
     * 查询指定 VIN 的首次激活事实（只读、幂等）
     *
     * @param vin 车架号（≤20）
     * @return 三态结论 + 事实证据（factRef 仅 RECORDED 时返回）
     */
    @GetMapping("/vin/{vin}/firstActivation")
    public VehicleActivationNodeExResponse getFirstActivation(@PathVariable String vin) {
        String normalizedVin = trimToNull(vin);
        if (normalizedVin == null) {
            // 参数非法与「无记录」严格区分（框架既有参数错误 100001）
            throw new InputParamInvalidException("vin不能为空");
        }
        if (normalizedVin.length() > 20) {
            throw new InputParamInvalidException("vin长度不能超过20");
        }
        long begin = System.currentTimeMillis();
        VehicleActivationDto dto = vehicleActivationQueryAppService.queryFirstActivation(normalizedVin);
        log.info("内部服务请求查询车辆[{}]首次激活事实, 结论={}, 耗时={}ms",
                maskVin(normalizedVin), dto.getState(), System.currentTimeMillis() - begin);

        return VehicleActivationNodeExResponse.builder()
                .state(dto.getState() == null ? VehicleActivationState.NOT_RECORDED : dto.getState())
                .vin(dto.getVin())
                .firstActivatedAt(dto.getFirstActivatedAt())
                .bindingOperationId(dto.getBindingOperationId())
                .sourceFactRef(dto.getSourceFactRef())
                .eventId(dto.getEventId())
                .occurredAt(dto.getOccurredAt())
                .receivedAt(dto.getReceivedAt())
                .vinResolved(dto.getVinResolved())
                .factRef(dto.getFactRef())
                .build();
    }

    /**
     * 去除首尾空白，空串归为 null
     *
     * @param value 原值
     * @return 规范化后的值
     */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * VIN 脱敏（保留前 3 后 4）
     *
     * @param vin 车架号
     * @return 脱敏后的 VIN
     */
    private static String maskVin(String vin) {
        if (vin == null || vin.length() <= 7) {
            return "****";
        }
        return vin.substring(0, 3) + "****" + vin.substring(vin.length() - 4);
    }

}
