package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.framework.common.enums.DeviceItem;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspCcpInfoService;
import net.hwyz.iov.cloud.iov.tsp.api.vo.CcpVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.BatchImportCcpRequest;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 中央计算平台数据解析器V1.0
 *
 * @author hwyz_leo
 */
@Slf4j
@RequiredArgsConstructor
@Component("ccpDataParserV1.0")
public class CcpDataParserV1_0 extends BaseParser implements ImportDataParser {

    private final TspCcpInfoService tspCcpInfoService;
    private final ImportDataParserRegistry parserRegistry;

    @PostConstruct
    public void init() {
        parserRegistry.register(this);
    }

    @Override
    public String getType() {
        return "CCP";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public ImportResult parse(String batchNum, JSONObject dataJson) {
        String supplier = getSupplier(dataJson);
        if (StrUtil.isBlank(supplier)) {
            log.warn("中央计算平台导入数据批次号[{}]供应商代码为空", batchNum);
        }
        JSONObject data = getData(dataJson);
        JSONArray items = data.getJSONArray("ITEMS");
        int totalCount = items.size();
        int invalidCount = 0;
        BatchImportCcpRequest request = new BatchImportCcpRequest();
        request.setBatchNum(batchNum);
        request.setSupplierCode(supplier);
        List<VehiclePart> vehiclePartList = new ArrayList<>();
        List<CcpVo> ccpList = new ArrayList<>();
        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            String pn = itemJson.getStr("NO");
            String sn = itemJson.getStr("SN");
            String hsm = itemJson.getStr("HSM");
            if (StrUtil.isBlank(pn) || StrUtil.isBlank(sn)) {
                invalidCount++;
                continue;
            }
            Map<String, Object> extra = new HashMap<>(1);
            extra.put("HSM", hsm);
            vehiclePartList.add(VehiclePart.builder()
                    .pn(pn)
                    .deviceCode(DeviceItem.CCP.name())
                    .deviceItem(DeviceItem.CCP.name())
                    .supplierCode(supplier)
                    .batchNum(batchNum)
                    .sn(sn)
                    .extra(JSONUtil.toJsonStr(extra))
                    .build());
            ccpList.add(CcpVo.builder()
                    .sn(sn)
                    .no(pn)
                    .hsm(hsm)
                    .build());
        }
        if (invalidCount > 0) {
            log.warn("中央计算平台导入数据批次号[{}]存在无效中央计算平台数据[{}]", batchNum, invalidCount);
        }
        int successCount = ccpList.size();
        createVehiclePart(vehiclePartList);
        request.setCcpList(ccpList);
        tspCcpInfoService.batchImport(request);
        return ImportResult.builder()
                .totalCount(totalCount)
                .successCount(successCount)
                .failureCount(0)
                .invalidCount(invalidCount)
                .build();
    }
}
