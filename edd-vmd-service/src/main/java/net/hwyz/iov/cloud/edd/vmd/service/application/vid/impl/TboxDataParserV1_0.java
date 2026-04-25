package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.framework.common.enums.DeviceItem;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.tsp.tbox.api.contract.TboxExService;
import net.hwyz.iov.cloud.tsp.tbox.api.contract.request.BatchImportTboxRequest;
import net.hwyz.iov.cloud.tsp.tbox.api.feign.service.ExTboxInfoService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车联终端数据解析器V1.0
 *
 * @author hwyz_leo
 */
@Slf4j
@RequiredArgsConstructor
@Component("tboxDataParserV1.0")
public class TboxDataParserV1_0 extends BaseParser implements ImportDataParser {

    private final ExTboxInfoService exTboxInfoService;

    @Override
    public void parse(String batchNum, JSONObject dataJson) {
        String supplier = getSupplier(dataJson);
        if (StrUtil.isBlank(supplier)) {
            log.warn("车联终端导入数据批次号[{}]供应商代码为空", batchNum);
        }
        JSONObject data = getData(dataJson);
        JSONArray items = data.getJSONArray("ITEMS");
        int tboxInvalidCount = 0;
        BatchImportTboxRequest request = new BatchImportTboxRequest();
        request.setBatchNum(batchNum);
        request.setSupplierCode(supplier);
        List<VehiclePart> vehiclePartList = new ArrayList<>();
        List<TboxExService> tboxList = new ArrayList<>();
        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            String pn = itemJson.getStr("NO");
            String sn = itemJson.getStr("SN");
            String iccid1 = itemJson.getStr("ICCID1");
            String iccid2 = itemJson.getStr("ICCID2");
            String imei = itemJson.getStr("IMEI");
            String hsm = itemJson.getStr("HSM");
            if (StrUtil.isBlank(pn) || StrUtil.isBlank(sn) || StrUtil.isAllBlank(iccid1, iccid2)) {
                tboxInvalidCount++;
                continue;
            }
            Map<String, Object> extra = new HashMap<>(4);
            extra.put("IMEI", imei);
            extra.put("ICCID1", iccid1);
            extra.put("ICCID2", iccid2);
            extra.put("HSM", hsm);
            vehiclePartList.add(VehiclePart.builder()
                    .pn(pn)
                    .deviceCode(DeviceItem.TBOX.name())
                    .deviceItem(DeviceItem.TBOX.name())
                    .supplierCode(supplier)
                    .batchNum(batchNum)
                    .sn(sn)
                    .extra(JSONUtil.toJsonStr(extra))
                    .build());
            tboxList.add(TboxExService.builder()
                    .sn(sn)
                    .no(pn)
                    .hsm(hsm)
                    .imei(imei)
                    .iccid1(iccid1)
                    .iccid2(iccid2)
                    .build());
        }
        if (tboxInvalidCount > 0) {
            log.warn("车联终端导入数据批次号[{}]存在无效车联终端数据[{}]", batchNum, tboxInvalidCount);
        }
        createVehiclePart(vehiclePartList);
        request.setTboxList(tboxList);
        exTboxInfoService.batchImport(request);
    }
}
