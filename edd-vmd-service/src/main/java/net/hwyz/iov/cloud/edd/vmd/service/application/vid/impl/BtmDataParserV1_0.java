package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.framework.common.enums.DeviceItem;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.tsp.idk.api.contract.BtmExService;
import net.hwyz.iov.cloud.tsp.idk.api.contract.request.BatchImportBtmRequest;
import net.hwyz.iov.cloud.tsp.idk.api.feign.service.ExBtmInfoService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehiclePartDo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 蓝牙模块数据解析器V1.0
 *
 * @author hwyz_leo
 */
@Slf4j
@RequiredArgsConstructor
@Component("btmDataParserV1.0")
public class BtmDataParserV1_0 extends BaseParser implements ImportDataParser {

    private final ExBtmInfoService exBtmInfoService;

    @Override
    public void parse(String batchNum, JSONObject dataJson) {
        String supplier = getSupplier(dataJson);
        if (StrUtil.isBlank(supplier)) {
            log.warn("蓝牙模块导入数据批次号[{}]供应商代码为空", batchNum);
        }
        JSONObject data = getData(dataJson);
        JSONArray items = data.getJSONArray("ITEMS");
        int btmInvalidCount = 0;
        BatchImportBtmRequest request = new BatchImportBtmRequest();
        request.setBatchNum(batchNum);
        request.setSupplierCode(supplier);
        List<VmdVehiclePartDo> vehiclePartList = new ArrayList<>();
        List<BtmExService> btmList = new ArrayList<>();
        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            String pn = itemJson.getStr("NO");
            String sn = itemJson.getStr("SN");
            String hsm = itemJson.getStr("HSM");
            String mac = itemJson.getStr("MAC");
            if (StrUtil.isBlank(sn)) {
                btmInvalidCount++;
                continue;
            }
            Map<String, Object> extra = new HashMap<>(2);
            extra.put("HSM", hsm);
            extra.put("MAC", mac);
            vehiclePartList.add(VmdVehiclePartDo.builder()
                    .pn(pn)
                    .deviceCode("BTM_M")
                    .deviceItem(DeviceItem.BTM.name())
                    .supplierCode(supplier)
                    .batchNum(batchNum)
                    .sn(sn)
                    .extra(JSONUtil.toJsonStr(extra))
                    .build());
            btmList.add(BtmExService.builder()
                    .sn(sn)
                    .no(pn)
                    .hsm(hsm)
                    .mac(mac)
                    .build());
        }
        if (btmInvalidCount > 0) {
            log.warn("蓝牙模块导入数据批次号[{}]存在无效蓝牙模块数据[{}]", batchNum, btmInvalidCount);
        }
        createVehiclePart(vehiclePartList);
        request.setBtmList(btmList);
        exBtmInfoService.batchImport(request);
    }
}
