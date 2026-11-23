package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.edd.vmd.api.fallback.VmdVehicleActivationServiceFallbackFactory;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehicleActivationNodeExResponse;
import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 车辆首次激活事实只读服务接口（VMD-DSN-CR-077 §7）
 * <p>
 * 纯只读契约：查询 per-VIN 首次激活生命周期事实投影，不写任何表、不发事件。
 * 三态见 {@link net.hwyz.iov.cloud.edd.vmd.api.vo.enums.VehicleActivationState}：
 * {@code RECORDED / PENDING / NOT_RECORDED} 均以 200 返回，禁用 404 表达「无事实」。
 * <p>
 * 技术失败 fail-closed：查询不可用时抛 806137（ACTIVATION_FACT_QUERY_UNAVAILABLE），
 * 绝不降级为 {@code NOT_RECORDED}（D-VMD-077-09）；Fallback 同样只抛错、不伪造结论。
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "vehicleActivationService", value = ServiceNameConstants.EDD_VMD,
        path = "/api/service/vehicleActivation/v1", fallbackFactory = VmdVehicleActivationServiceFallbackFactory.class)
public interface VmdVehicleActivationService {

    /**
     * 查询指定 VIN 的首次激活事实（只读、幂等、无写副作用）
     *
     * @param vin 车架号（≤20，VMD 存储口径）
     * @return 三态结论 + 事实证据（factRef 仅在 RECORDED 时返回）
     */
    @GetMapping("/vin/{vin}/firstActivation")
    VehicleActivationNodeExResponse getFirstActivation(@PathVariable String vin);

}
