package net.hwyz.iov.cloud.edd.vmd.service.application.event.subscribe;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleEolPartBoundEvent;
import net.hwyz.iov.cloud.framework.common.enums.DeviceItem;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.ota.api.service.OtaVehiclePartService;
import net.hwyz.iov.cloud.iov.ota.api.vo.VehiclePartExService;
import net.hwyz.iov.cloud.iov.ota.api.vo.request.SaveVehiclePartsRequest;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspVehicleCcpService;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspVehicleNetworkService;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspVehicleTboxService;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleCcpVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleNetworkVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleTboxVo;
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

    private final TspVehicleCcpService tspVehicleCcpService;
    private final OtaVehiclePartService otaVehiclePartService;
    private final TspVehicleTboxService tspVehicleTboxService;
    private final TspVehicleNetworkService tspVehicleNetworkService;

    /**
     * 异步订阅车辆下线零件绑定事件
     *
     * @param event 车辆下线零件绑定完成事件
     */
    @Async
    @EventListener
    public void onVehicleEolPartBoundEvent(VehicleEolPartBoundEvent event) {
        String vin = event.getVin();
        List<VehicleEolPartBoundEvent.PartMeta> parts = event.getParts();
        log.info("异步处理车辆[{}]下线零件绑定事件，零件数[{}]", vin, parts.size());
        // 通知 TSP
        for (VehicleEolPartBoundEvent.PartMeta part : parts) {
            try {
                notifyTsp(vin, part);
            } catch (Exception e) {
                log.warn("车辆[{}]零件[{}]通知TSP异常", vin, part.getDeviceCode(), e);
            }
        }
        // 同步零件列表到 OTA
        try {
            notifyOta(vin, parts);
        } catch (Exception e) {
            log.warn("车辆[{}]同步零件到OTA异常", vin, e);
        }
    }

    private void notifyTsp(String vin, VehicleEolPartBoundEvent.PartMeta part) {
        String deviceItem = part.getDeviceItem();
        if (DeviceItem.TBOX.name().equalsIgnoreCase(deviceItem)) {
            if (StrUtil.isNotBlank(part.getIccid1())) {
                tspVehicleNetworkService.create(VehicleNetworkVo.builder()
                        .vin(vin)
                        .iccid1(part.getIccid1())
                        .iccid2(part.getIccid2())
                        .build());
            }
            tspVehicleTboxService.bind(VehicleTboxVo.builder().vin(vin).sn(part.getSn()).build());
        } else if (DeviceItem.CCP.name().equalsIgnoreCase(deviceItem)) {
            tspVehicleCcpService.bind(VehicleCcpVo.builder().vin(vin).sn(part.getSn()).build());
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
                    .pn(part.getPn())
                    .deviceCode(part.getDeviceCode())
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
