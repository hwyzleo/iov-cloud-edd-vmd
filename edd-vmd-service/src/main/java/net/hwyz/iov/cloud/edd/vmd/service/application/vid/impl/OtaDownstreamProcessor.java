package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessor;
import net.hwyz.iov.cloud.iov.ota.api.service.OtaVehiclePartService;
import net.hwyz.iov.cloud.iov.ota.api.vo.VehiclePartExService;
import net.hwyz.iov.cloud.iov.ota.api.vo.request.SaveVehiclePartsRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * OTA下游处理器
 * 处理OTA相关的下游联动
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtaDownstreamProcessor implements DownstreamProcessor {

    private final OtaVehiclePartService otaVehiclePartService;

    @Override
    public void process(String batchNum, String partCode, String vehicleNodeCode, JSONObject data) {
        log.info("OTA处理器处理零件导入下游联动, batchNum={}, partCode={}, vehicleNodeCode={}", 
                batchNum, partCode, vehicleNodeCode);
        
        String vin = data.getStr("vin");
        String sn = data.getStr("sn");
        String deviceItem = data.getStr("deviceItem");
        String supplierCode = data.getStr("supplierCode");
        String configWord = data.getStr("configWord");
        String hardwareVer = data.getStr("hardwareVer");
        String softwareVer = data.getStr("softwareVer");
        String hardwarePn = data.getStr("hardwarePn");
        String softwarePn = data.getStr("softwarePn");
        
        SaveVehiclePartsRequest request = new SaveVehiclePartsRequest();
        request.setVin(vin);
        request.setRemark("零件实例导入");
        
        List<VehiclePartExService> vehiclePartList = new ArrayList<>();
        vehiclePartList.add(VehiclePartExService.builder()
                .sn(sn)
                .pn(partCode)
                .deviceCode(vehicleNodeCode)
                .deviceItem(deviceItem)
                .supplierCode(supplierCode)
                .batchNum(batchNum)
                .configWord(configWord)
                .hardwareVer(hardwareVer)
                .softwareVer(softwareVer)
                .hardwarePn(hardwarePn)
                .softwarePn(softwarePn)
                .build());
        
        request.setVehiclePartList(vehiclePartList);
        otaVehiclePartService.saveVehicleParts(vin, request);
        
        log.info("OTA处理器处理完成, batchNum={}, partCode={}", batchNum, partCode);
    }

    @Override
    public String getSupportedVehicleNodeCode() {
        return "OTA";
    }
}
