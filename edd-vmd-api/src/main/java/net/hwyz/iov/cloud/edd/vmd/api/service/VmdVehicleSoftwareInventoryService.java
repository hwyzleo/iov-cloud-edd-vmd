package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.edd.vmd.api.vo.request.SoftwareManifestRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.SoftwareInventoryExResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.SoftwareManifestApplyResponse;
import net.hwyz.iov.cloud.edd.vmd.api.fallback.VmdVehicleSoftwareInventoryServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 车辆软件实装清单服务接口
 * <p>
 * 提供软件清单查询和写入接口
 * 供下游 OTA 建立只读投影（增量靠 VehicleSoftwareInventoryChangedEvent）
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "vehicleSoftwareInventoryService", value = ServiceNameConstants.EDD_VMD, path = "/api/service/softwareInventory/v1", fallbackFactory = VmdVehicleSoftwareInventoryServiceFallbackFactory.class)
public interface VmdVehicleSoftwareInventoryService {

    /**
     * 查询车辆当前软件实装清单
     *
     * @param vin 车架号
     * @return 软件实装清单列表
     */
    @GetMapping("/vin/{vin}/current")
    List<SoftwareInventoryExResponse> getCurrentSoftwareInventory(@PathVariable String vin);

    /**
     * 查询车辆软件版本变化历史
     *
     * @param vin 车架号
     * @param vehicleNodeCode 车载节点代码（可选）
     * @param softwareTargetCode 软件目标代码（可选）
     * @return 软件版本变化时间线
     */
    @GetMapping("/vin/{vin}/history")
    List<SoftwareInventoryExResponse> getSoftwareHistory(
            @PathVariable String vin,
            @RequestParam(required = false) String vehicleNodeCode,
            @RequestParam(required = false) String softwareTargetCode);

    /**
     * 全量/增量分页拉取当前软件清单
     * <p>
     * 供 OTA Bootstrap 与丢事件修复
     *
     * @param cursor 分页游标（可选，首次拉取不传）
     * @param updatedAfter 增量拉取起始时间（可选）
     * @param size 每页大小（默认 100）
     * @return 软件实装清单列表
     */
    @GetMapping("/bootstrap")
    List<SoftwareInventoryExResponse> listCurrentSoftwareInventory(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) String updatedAfter,
            @RequestParam(defaultValue = "100") Integer size);

    /**
     * 外部来源同步回写软件实装清单
     * <p>
     * 经 SoftwareInventoryAppService.applyManifest 走版本时序 gate
     * → provisional/confirmed → 来源优先级消解
     *
     * @param request 软件实装清单写入请求
     * @return 写入结果
     */
    @PostMapping("/apply")
    SoftwareManifestApplyResponse applySoftwareManifest(@RequestBody SoftwareManifestRequest request);

}
