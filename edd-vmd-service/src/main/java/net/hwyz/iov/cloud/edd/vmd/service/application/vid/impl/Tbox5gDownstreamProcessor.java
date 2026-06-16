package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessor;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspTboxInfoService;
import net.hwyz.iov.cloud.iov.tsp.api.vo.TboxVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.BatchImportTboxRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TSP下游处理器
 * 处理TSP相关的下游联动
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Tbox5gDownstreamProcessor extends BaseProcessor implements DownstreamProcessor {

    private final TspTboxInfoService tspTboxInfoService;

    @Override
    public void process(String batchNum, String partCode, String vehicleNodeCode, JSONObject data) {
        log.info("TSP处理器处理零件导入下游联动, batchNum={}, partCode={}, vehicleNodeCode={}",
                batchNum, partCode, vehicleNodeCode);

        String supplier = getSupplier(data);
        if (StrUtil.isBlank(supplier)) {
            log.warn("车联终端导入数据批次号[{}]供应商代码为空", batchNum);
        }
        JSONObject dataContent = getData(data);
        JSONArray items = dataContent.getJSONArray("ITEMS");

        List<PartInboundAppService.PartInboundRecord> records = new ArrayList<>();
        List<TboxVo> tboxList = new ArrayList<>();
        int invalidCount = 0;

        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            String pn = itemJson.getStr("NO");
            String sn = itemJson.getStr("SN");
            String iccid1 = itemJson.getStr("ICCID1");
            String iccid2 = itemJson.getStr("ICCID2");
            String imei = itemJson.getStr("IMEI");
            String hsm = itemJson.getStr("HSM");

            if (StrUtil.isBlank(pn) || StrUtil.isBlank(sn) || StrUtil.isAllBlank(iccid1, iccid2)) {
                invalidCount++;
                continue;
            }

            Map<String, String> extraFields = new HashMap<>(4);
            extraFields.put("imei", imei);
            extraFields.put("iccid1", iccid1);
            extraFields.put("iccid2", iccid2);
            extraFields.put("hsm", hsm);

            PartInboundAppService.PartInboundRecord record = buildInboundRecord(pn, sn, "TBOX", "TBOX", "TBOX",
                    supplier, batchNum, extraFields);
            records.add(record);

            tboxList.add(TboxVo.builder()
                    .sn(sn)
                    .no(pn)
                    .hsm(hsm)
                    .imei(imei)
                    .iccid1(iccid1)
                    .iccid2(iccid2)
                    .build());
        }

        if (invalidCount > 0) {
            log.warn("车联终端导入数据批次号[{}]存在无效车联终端数据[{}]", batchNum, invalidCount);
        }

        // 调用下游TSP服务
        if (!tboxList.isEmpty()) {
            BatchImportTboxRequest request = new BatchImportTboxRequest();
            request.setBatchNum(batchNum);
            request.setSupplierCode(supplier);
            request.setTboxList(tboxList);
            tspTboxInfoService.batchImport(request);
        }

        log.info("TSP处理器处理完成, batchNum={}, partCode={}", batchNum, partCode);
    }

    @Override
    public String getSupportedVehicleNodeCode() {
        return "TBOX_5G";
    }
}