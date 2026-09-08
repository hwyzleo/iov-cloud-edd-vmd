package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 车辆软件观测 active 物理绑定解析器
 * <p>
 * VMD-DSN-CR-046: 按 vin + ecuId 解析唯一 active vehicle_part，
 * 返回 bindingId + partId。无唯一 active 绑定时不自动建绑定，
 * 由消费者隔离该 item 并告警。
 *
 * @author hwyz_leo
 * @since 2026-09-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleSoftwareBindingResolver {

    private final VehiclePartAppService vehiclePartAppService;

    /**
     * 解析唯一 active 物理绑定
     *
     * @param vin  车架号
     * @param ecuId ECU 标识（= vehicleNodeCode）
     * @return 绑定解析结果
     * @throws ActiveBindingNotFoundException 无 active 绑定（隔离）
     * @throws MultipleActiveBindingException 存在多个 active 绑定（隔离）
     */
    public BindingResolution resolve(String vin, String ecuId) {
        List<VehiclePart> activeBindings = vehiclePartAppService.getActiveBindingsByVin(vin);
        if (activeBindings == null || activeBindings.isEmpty()) {
            log.warn("车辆[{}]无 active 绑定，ecuId[{}]隔离", vin, ecuId);
            throw new ActiveBindingNotFoundException(vin, ecuId);
        }

        List<VehiclePart> matched = activeBindings.stream()
                .filter(binding -> ecuId.equals(binding.getVehicleNodeCode()))
                .toList();

        if (matched.isEmpty()) {
            log.warn("车辆[{}]未找到节点[{}]的 active 绑定，隔离", vin, ecuId);
            throw new ActiveBindingNotFoundException(vin, ecuId);
        }
        if (matched.size() > 1) {
            log.warn("车辆[{}]节点[{}]存在 {} 条 active 绑定，隔离", vin, ecuId, matched.size());
            throw new MultipleActiveBindingException(vin, ecuId, matched.size());
        }

        VehiclePart binding = matched.get(0);
        log.debug("车辆[{}]节点[{}]解析唯一绑定: bindingId={}, partId={}",
                vin, ecuId, binding.getId(), binding.getPartId());
        return new BindingResolution(binding.getId(), binding.getPartId());
    }

    /**
     * 绑定解析结果
     *
     * @param bindingId 绑定ID（= vehicle_part.id）
     * @param partId    零件ID（= part_info.id）
     */
    public record BindingResolution(Long bindingId, Long partId) {
    }

    /**
     * 无 active 绑定异常（隔离条件，非错误码异常）
     */
    public static class ActiveBindingNotFoundException extends RuntimeException {

        public ActiveBindingNotFoundException(String vin, String ecuId) {
            super("车辆[" + vin + "]节点[" + ecuId + "]无唯一active绑定");
        }
    }

    /**
     * 多 active 绑定异常（隔离条件，非错误码异常）
     */
    public static class MultipleActiveBindingException extends RuntimeException {

        public MultipleActiveBindingException(String vin, String ecuId, int count) {
            super("车辆[" + vin + "]节点[" + ecuId + "]存在" + count + "条active绑定");
        }
    }
}
