package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessor;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspVehicleCcpService;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspVehicleNetworkService;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspVehicleTboxService;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleCcpVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleNetworkVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.VehicleTboxVo;
import net.hwyz.iov.cloud.framework.common.enums.DeviceItem;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import org.springframework.stereotype.Component;

/**
 * TSP下游处理器
 * 处理TSP相关的下游联动
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TspDownstreamProcessor implements DownstreamProcessor {

    private final TspVehicleCcpService tspVehicleCcpService;
    private final TspVehicleNetworkService tspVehicleNetworkService;
    private final TspVehicleTboxService tspVehicleTboxService;

    @Override
    public void process(String batchNum, String partCode, String vehicleNodeCode, JSONObject data) {
        log.info("TSP处理器处理零件导入下游联动, batchNum={}, partCode={}, vehicleNodeCode={}", 
                batchNum, partCode, vehicleNodeCode);
        
        String vin = data.getStr("vin");
        String sn = data.getStr("sn");
        String deviceItem = data.getStr("deviceItem");
        
        if (DeviceItem.TBOX.name().equalsIgnoreCase(deviceItem)) {
            // TBOX设备处理
            String iccid1 = data.getStr("iccid1");
            String iccid2 = data.getStr("iccid2");
            if (StrUtil.isNotBlank(iccid1)) {
                tspVehicleNetworkService.create(VehicleNetworkVo.builder()
                        .vin(vin)
                        .iccid1(iccid1)
                        .iccid2(iccid2)
                        .build());
            }
            tspVehicleTboxService.bind(VehicleTboxVo.builder().vin(vin).sn(sn).build());
        } else if (DeviceItem.CCP.name().equalsIgnoreCase(deviceItem)) {
            // CCP设备处理
            tspVehicleCcpService.bind(VehicleCcpVo.builder().vin(vin).sn(sn).build());
        }
        
        log.info("TSP处理器处理完成, batchNum={}, partCode={}", batchNum, partCode);
    }

    @Override
    public String getSupportedVehicleNodeCode() {
        return "TSP";
    }
}