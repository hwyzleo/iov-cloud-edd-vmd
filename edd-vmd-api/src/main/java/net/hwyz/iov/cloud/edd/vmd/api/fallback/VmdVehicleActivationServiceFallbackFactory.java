package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdVehicleActivationService;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehicleActivationNodeExResponse;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 车辆首次激活事实只读服务降级处理（VMD-DSN-CR-077 §7.3）
 * <p>
 * fail-closed：降级时<strong>抛错</strong>，绝不返回 {@code state=NOT_RECORDED}
 * —— 把「查询不可用」伪装成「未激活」会让调用方做出错误的业务结论（如重复执行激活）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class VmdVehicleActivationServiceFallbackFactory implements FallbackFactory<VmdVehicleActivationService> {

    @Override
    public VmdVehicleActivationService create(Throwable throwable) {
        return new VmdVehicleActivationService() {
            @Override
            public VehicleActivationNodeExResponse getFirstActivation(String vin) {
                // 脱敏：不输出完整 VIN（保留前 3 后 4）
                log.error("车辆首次激活事实查询服务按车架号[{}]查询失败（fail-closed，不返回 NOT_RECORDED）",
                        maskVin(vin), throwable);
                throw new RuntimeException("车辆首次激活事实查询服务不可用", throwable);
            }
        };
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
