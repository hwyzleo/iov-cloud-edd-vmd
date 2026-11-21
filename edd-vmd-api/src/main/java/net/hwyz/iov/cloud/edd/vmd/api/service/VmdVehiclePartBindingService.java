package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehiclePartBindingByDeviceExResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehiclePartBindingExResponse;
import net.hwyz.iov.cloud.edd.vmd.api.fallback.VmdVehiclePartBindingServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 车辆-零件绑定关系服务接口
 * <p>
 * 提供按 VIN 查询 active 绑定和全量/增量 bootstrap 接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "vehiclePartBindingService", value = ServiceNameConstants.EDD_VMD, path = "/api/service/vehiclePartBinding/v1", fallbackFactory = VmdVehiclePartBindingServiceFallbackFactory.class)
public interface VmdVehiclePartBindingService {

    /**
     * 根据车架号查询当前全部 active 设备绑定
     *
     * @param vin 车架号
     * @return active 绑定列表
     */
    @GetMapping("/vin/{vin}/active")
    List<VehiclePartBindingExResponse> listActiveBindingsByVin(@PathVariable String vin);

    /**
     * 全量/增量分页拉取 active 绑定
     * <p>
     * 供下游首次 bootstrap 与丢事件重建
     *
     * @param cursor 分页游标（可选，首次拉取不传）
     * @param size   每页大小（默认 100）
     * @return active 绑定列表
     */
    @GetMapping("/bootstrap")
    List<VehiclePartBindingExResponse> listBindingsForBootstrap(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "100") Integer size);

    /**
     * 根据设备标识查询当前 active 绑定的车辆（VMD-DSN-CR-076）
     * <p>
     * 只读、幂等、无写副作用；仅匹配 bind_state=1 且 row_valid=1 的绑定事实。
     * 三态结论见 {@link net.hwyz.iov.cloud.edd.vmd.api.vo.enums.VehicleBindingState}：
     * 无绑定与数据冲突均返回 200 + 状态，不使用 404 或空对象；冲突不返回 VIN。
     * 参数缺失/非法返回框架既有参数错误（100001），不混同为「无绑定」。
     * </p>
     *
     * @param deviceId       设备序列号（= 绑定事实 tb_part_info.sn）
     * @param deviceCategory 设备类别（如 IVI；快照优先、MDM 节点类别回退）
     * @return 设备→当前绑定车辆只读结果
     */
    @GetMapping("/vehicleByDevice")
    VehiclePartBindingByDeviceExResponse getVehicleByDevice(
            @RequestParam String deviceId,
            @RequestParam String deviceCategory);

}