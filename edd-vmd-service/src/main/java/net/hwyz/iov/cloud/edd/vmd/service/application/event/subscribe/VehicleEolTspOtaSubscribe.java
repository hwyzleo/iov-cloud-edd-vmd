package net.hwyz.iov.cloud.edd.vmd.service.application.event.subscribe;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleEolPartBoundEvent;
import net.hwyz.iov.cloud.framework.common.enums.DeviceItem;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.ota.api.service.OtaVehiclePartService;
import net.hwyz.iov.cloud.iov.ota.api.vo.VehiclePartExService;
import net.hwyz.iov.cloud.iov.ota.api.vo.request.SaveVehiclePartsRequest;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 车辆下线零件绑定事件订阅 - 异步通知 TSP/OTA 下游服务
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleEolTspOtaSubscribe {

    private final OtaVehiclePartService otaVehiclePartService;

    /**
     * 异步订阅车辆下线零件绑定事件
     * <p>
     * CR-033: VMD 不再主动 Feign 写 TSP 绑定/网络，改为发布绑定事件。
     * OTA 联动建议迁移为订阅本事件，迁移节奏由 OTA 侧 CR 跟进。
     *
     * @param event 车辆下线零件绑定完成事件
     */
    @Async
    @EventListener
    public void onVehicleEolPartBoundEvent(VehicleEolPartBoundEvent event) {
        String vin = event.getVin();
        List<VehicleEolPartBoundEvent.PartMeta> parts = event.getParts();
        log.info("异步处理车辆[{}]下线零件绑定事件，零件数[{}]", vin, parts.size());
        // 同步零件列表到 OTA（OTA 联动建议迁移为订阅本事件，迁移节奏由 OTA 侧 CR 跟进）
        try {
            notifyOta(vin, parts);
        } catch (Exception e) {
            log.warn("车辆[{}]同步零件到OTA异常", vin, e);
        }
    }

    private void notifyOta(String vin, List<VehicleEolPartBoundEvent.PartMeta> parts) {
        SaveVehiclePartsRequest request = new SaveVehiclePartsRequest();
        request.setVin(vin);
        request.setRemark("车辆下线");
        List<VehiclePartExService> vehiclePartList = new ArrayList<>();
        for (VehicleEolPartBoundEvent.PartMeta part : parts) {
            vehiclePartList.add(VehiclePartExService.builder()
                    .sn(part.getSn())
                    .pn(part.getCode())
                    .deviceCode(part.getVehicleNodeCode())
                    .deviceItem(part.getDeviceItem())
                    .supplierCode(part.getSupplierCode())
                    .batchNum(part.getBatchNum())
                    .configWord(part.getConfigWord())
                    .hardwareVer(part.getHardwareVer())
                    .softwareVer(part.getSoftwareVer())
                    .hardwarePn(part.getHardwarePn())
                    .softwarePn(part.getSoftwarePn())
                    .build());
        }
        request.setVehiclePartList(vehiclePartList);
        otaVehiclePartService.saveVehicleParts(vin, request);
    }

}
