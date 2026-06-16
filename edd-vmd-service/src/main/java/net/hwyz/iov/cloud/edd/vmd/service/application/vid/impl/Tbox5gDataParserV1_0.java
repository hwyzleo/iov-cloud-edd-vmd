package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService.PartInboundRecord;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService.PartInboundResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.InboundSourceType;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspTboxInfoService;
import net.hwyz.iov.cloud.iov.tsp.api.vo.TboxVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.BatchImportTboxRequest;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 5G车载远程通信终端数据解析器V1.0
 *
 * @author hwyz_leo
 */
@Slf4j
@RequiredArgsConstructor
@Component("tbox5gDataParserV1.0")
public class Tbox5gDataParserV1_0 extends BaseParser implements ImportDataParser {

    private final TspTboxInfoService tspTboxInfoService;
    private final ImportDataParserRegistry parserRegistry;

    @PostConstruct
    public void init() {
        parserRegistry.register(this);
    }

    @Override
    public String getType() {
        return "TBOX_5G";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public ImportResult parse(String batchNum, JSONObject dataJson) {
        String supplier = getSupplier(dataJson);
        if (StrUtil.isBlank(supplier)) {
            log.warn("车联终端导入数据批次号[{}]供应商代码为空", batchNum);
        }
        JSONObject data = getData(dataJson);
        JSONArray items = data.getJSONArray("ITEMS");

        List<PartInboundRecord> records = new ArrayList<>();
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

            PartInboundRecord record = buildInboundRecord(pn, sn, "TBOX", "TBOX", "TBOX",
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

        // 使用入站内核处理
        PartInboundResult result = partInboundAppService.processInbound(records, InboundSourceType.MES, null);

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

        return ImportResult.builder()
                .totalCount(result.getTotalCount())
                .successCount(result.getSuccessCount())
                .failureCount(result.getFailureCount())
                .invalidCount(invalidCount)
                .build();
    }
}
