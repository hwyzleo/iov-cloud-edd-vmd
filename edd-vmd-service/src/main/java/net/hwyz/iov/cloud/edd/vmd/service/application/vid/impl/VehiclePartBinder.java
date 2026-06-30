package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleEolPartBoundEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInfoAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleNodeAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartInstanceState;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆零件绑定组件
 * <p>
 * 封装 EOL 零件的遍历、校验、入库绑定逻辑。
 * 返回零件元数据列表供事件发布使用。
 * <p>
 * CR-022: 两步 upsert + 乱序兜底
 * 1. 先按 (partCode, sn) upsert part_info（幂等）
 * 2. VIN + 安装位置就绪时 upsert vehicle_part 绑定
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VehiclePartBinder {

    private final PartInfoAppService partInfoAppService;
    private final VehiclePartAppService vehiclePartAppService;
    private final VehicleNodeAppService vehicleNodeAppService;

    /**
     * 绑定零件列表
     *
     * @param parts 零件 JSON 数组
     * @param vin 车架号
     * @param batchNum 批次号
     * @return 绑定成功的零件元数据列表
     */
    public List<VehicleEolPartBoundEvent.PartMeta> bindParts(JSONArray parts, String vin, String batchNum) {
        List<VehicleEolPartBoundEvent.PartMeta> partMetaList = new ArrayList<>();
        for (Object part : parts) {
            JSONObject partJson = JSONUtil.parseObj(part);
            String deviceCode = partJson.getStr("DEVICE_CODE");
            if (StrUtil.isBlank(deviceCode)) {
                log.warn("零件导入数据批次号[{}]车架号[{}]设备[{}]为空", batchNum, vin, deviceCode);
                continue;
            }
            String partVin = partJson.getStr("VIN");
            if (!vin.equalsIgnoreCase(partVin)) {
                log.warn("零件导入数据批次号[{}]车架号[{}]设备[{}]车架号[{}]不一致", batchNum, vin, deviceCode, partVin);
                continue;
            }
            String pn = partJson.getStr("PART_NO");
            String sn = partJson.getStr("PART_SN");
            VehicleNode vehicleNode = vehicleNodeAppService.getVehicleNodeByCode(deviceCode);
            String supplierCode = partJson.getStr("SUPPLIER_CODE");
            String configWord = partJson.getStr("CONFIG_WORD");
            String hardwareVersion = partJson.getStr("HARDWARE_VERSION");
            String softwareVersion = partJson.getStr("SOFTWARE_VERSION");
            String hardwarePn = partJson.getStr("HARDWARE_PN");
            String softwarePn = partJson.getStr("SOFTWARE_PN");
            String iccid1 = partJson.getStr("ICCID1");
            String iccid2 = partJson.getStr("ICCID2");
            if (ObjUtil.isNull(vehicleNode)) {
                log.warn("零件导入数据批次号[{}]车架号[{}]设备[{}]异常", batchNum, vin, deviceCode);
            }
            String deviceItem = vehicleNode != null ? vehicleNode.getDeviceCategory() : null;
            partMetaList.add(new VehicleEolPartBoundEvent.PartMeta(
                    sn, pn, deviceCode, deviceItem, supplierCode, batchNum,
                    configWord, hardwareVersion, softwareVersion, hardwarePn, softwarePn,
                    iccid1, iccid2));
            try {
                // 补偿绑定语义：TOL 已绑跳过、漏绑补绑
                VehiclePart existingBinding = vehiclePartAppService.findByVinAndPosition(vin, deviceCode);
                if (existingBinding != null && existingBinding.getBindState() == 1) {
                    // TOL 已绑，跳过
                    log.debug("车辆[{}]位置[{}]已绑定零件[{}]，跳过", vin, deviceCode, pn);
                    // 回填零件细节（软件版本/配置字）入 part_info.extra
                    Map<String, String> extraFields = buildExtraFields(configWord, hardwareVersion, softwareVersion, hardwarePn, softwarePn, iccid1, iccid2);
                    partInfoAppService.updateExtraFields(pn, sn, extraFields);
                    continue;
                }

                // 步骤1: 先按 (partCode, sn) upsert part_info（幂等）
                PartInfo partInfo = PartInfo.builder()
                        .partCode(pn)
                        .sn(sn)
                        .vehicleNodeCode(deviceCode)
                        .supplierCode(supplierCode)
                        .batchNum(batchNum)
                        .configWord(configWord)
                        .hardwareVer(hardwareVersion)
                        .softwareVer(softwareVersion)
                        .hardwarePn(hardwarePn)
                        .softwarePn(softwarePn)
                        .instanceState(PartInstanceState.IN_USE.value)
                        .build();
                partInfoAppService.upsertPartInfo(partInfo);

                // 回填零件细节（软件版本、配置字/变体编码等）入 part_info.extra
                Map<String, String> extraFields = buildExtraFields(configWord, hardwareVersion, softwareVersion, hardwarePn, softwarePn, iccid1, iccid2);
                if (!extraFields.isEmpty()) {
                    partInfoAppService.updateExtraFields(pn, sn, extraFields);
                }

                // 步骤2: VIN + 安装位置就绪时 upsert vehicle_part 绑定
                VehiclePart vehiclePart = VehiclePart.builder()
                        .vin(vin)
                        .partId(partInfo.getId())
                        .vehicleNodeCode(deviceCode)
                        .deviceItem(deviceItem)
                        .bindOrg("MES")
                        .build();
                vehiclePartAppService.bindVehiclePart(vehiclePart);
            } catch (Exception e) {
                log.warn("零件导入数据批次号[{}]车架号[{}]零部件[{}]绑定异常", batchNum, vin, deviceCode, e);
            }
        }
        return partMetaList;
    }

    /**
     * 构建额外字段 Map
     *
     * @param configWord 配置字
     * @param hardwareVersion 硬件版本
     * @param softwareVersion 软件版本
     * @param hardwarePn 硬件零件号
     * @param softwarePn 软件零件号
     * @param iccid1 ICCID1
     * @param iccid2 ICCID2
     * @return 额外字段 Map
     */
    private Map<String, String> buildExtraFields(String configWord, String hardwareVersion, String softwareVersion,
                                                  String hardwarePn, String softwarePn, String iccid1, String iccid2) {
        Map<String, String> extraFields = new HashMap<>();
        if (configWord != null) extraFields.put("configWord", configWord);
        if (hardwareVersion != null) extraFields.put("hardwareVersion", hardwareVersion);
        if (softwareVersion != null) extraFields.put("softwareVersion", softwareVersion);
        if (hardwarePn != null) extraFields.put("hardwarePn", hardwarePn);
        if (softwarePn != null) extraFields.put("softwarePn", softwarePn);
        if (iccid1 != null) extraFields.put("iccid1", iccid1);
        if (iccid2 != null) extraFields.put("iccid2", iccid2);
        return extraFields;
    }
}
