package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.framework.common.enums.DeviceItem;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.tsp.idcm.api.contract.IdcmExService;
import net.hwyz.iov.cloud.tsp.idcm.api.contract.request.BatchImportIdcmRequest;
import net.hwyz.iov.cloud.tsp.idcm.api.feign.service.ExIdcmInfoService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehiclePartPo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 信息娱乐模块数据解析器V1.0
 *
 * @author hwyz_leo
 */
@Slf4j
@RequiredArgsConstructor
@Component("idcmDataParserV1.0")
public class IdcmDataParserV1_0 extends BaseParser implements ImportDataParser {

    private final ExIdcmInfoService exIdcmInfoService;

    @Override
    public void parse(String batchNum, JSONObject dataJson) {
        String supplier = getSupplier(dataJson);
        if (StrUtil.isBlank(supplier)) {
            log.warn("信息娱乐模块导入数据批次号[{}]供应商代码为空", batchNum);
        }
        JSONObject data = getData(dataJson);
        JSONArray items = data.getJSONArray("ITEMS");
        int idcmInvalidCount = 0;
        BatchImportIdcmRequest request = new BatchImportIdcmRequest();
        request.setBatchNum(batchNum);
        request.setSupplierCode(supplier);
        List<VehiclePartPo> vehiclePartList = new ArrayList<>();
        List<IdcmExService> idcmList = new ArrayList<>();
        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            String pn = itemJson.getStr("NO");
            String sn = itemJson.getStr("SN");
            String hsm = itemJson.getStr("HSM");
            String mac = itemJson.getStr("MAC");
            if (StrUtil.isBlank(sn)) {
                idcmInvalidCount++;
                continue;
            }
            Map<String, Object> extra = new HashMap<>(2);
            extra.put("HSM", hsm);
            extra.put("MAC", mac);
            vehiclePartList.add(VehiclePartPo.builder()
                    .pn(pn)
                    .deviceCode(DeviceItem.IDCM.name())
                    .deviceItem(DeviceItem.IDCM.name())
                    .supplierCode(supplier)
                    .batchNum(batchNum)
                    .sn(sn)
                    .extra(JSONUtil.toJsonStr(extra))
                    .build());
            idcmList.add(IdcmExService.builder()
                    .sn(sn)
                    .no(pn)
                    .hsm(hsm)
                    .mac(mac)
                    .build());
        }
        if (idcmInvalidCount > 0) {
            log.warn("信息娱乐模块导入数据批次号[{}]存在无效信息娱乐模块数据[{}]", batchNum, idcmInvalidCount);
        }
        createVehiclePart(vehiclePartList);
        request.setIdcmList(idcmList);
        exIdcmInfoService.batchImport(request);
    }
}
