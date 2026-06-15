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
import net.hwyz.iov.cloud.iov.tsp.api.service.TspIdcmInfoService;
import net.hwyz.iov.cloud.iov.tsp.api.vo.IdcmVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.BatchImportIdcmRequest;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 座舱域控数据解析器V1.0
 *
 * @author hwyz_leo
 */
@Slf4j
@RequiredArgsConstructor
@Component("cptDcuDataParserV1.0")
public class CptDcuDataParserV1_0 extends BaseParser implements ImportDataParser {

    private final TspIdcmInfoService tspIdcmInfoService;
    private final ImportDataParserRegistry parserRegistry;

    @PostConstruct
    public void init() {
        parserRegistry.register(this);
    }

    @Override
    public String getType() {
        return "CPT_DCU";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public ImportResult parse(String batchNum, JSONObject dataJson) {
        String supplier = getSupplier(dataJson);
        if (StrUtil.isBlank(supplier)) {
            log.warn("信息娱乐模块导入数据批次号[{}]供应商代码为空", batchNum);
        }
        JSONObject data = getData(dataJson);
        JSONArray items = data.getJSONArray("ITEMS");

        List<PartInboundRecord> records = new ArrayList<>();
        List<IdcmVo> idcmList = new ArrayList<>();
        int invalidCount = 0;

        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            String pn = itemJson.getStr("NO");
            String sn = itemJson.getStr("SN");
            String hsm = itemJson.getStr("HSM");
            String mac = itemJson.getStr("MAC");

            if (StrUtil.isBlank(sn)) {
                invalidCount++;
                continue;
            }

            Map<String, String> extraFields = new HashMap<>(2);
            extraFields.put("hsm", hsm);
            extraFields.put("mac", mac);

            PartInboundRecord record = buildInboundRecord(pn, sn, "IDCM", "IDCM", "IDCM",
                    supplier, batchNum, extraFields);
            records.add(record);

            idcmList.add(IdcmVo.builder()
                    .sn(sn)
                    .no(pn)
                    .hsm(hsm)
                    .mac(mac)
                    .build());
        }

        // 使用入站内核处理
        PartInboundResult result = partInboundAppService.processInbound(records, InboundSourceType.MES, null);

        if (invalidCount > 0) {
            log.warn("信息娱乐模块导入数据批次号[{}]存在无效信息娱乐模块数据[{}]", batchNum, invalidCount);
        }

        // 调用下游TSP服务
        if (!idcmList.isEmpty()) {
            BatchImportIdcmRequest request = new BatchImportIdcmRequest();
            request.setBatchNum(batchNum);
            request.setSupplierCode(supplier);
            request.setIdcmList(idcmList);
            tspIdcmInfoService.batchImport(request);
        }

        return ImportResult.builder()
                .totalCount(result.getTotalCount())
                .successCount(result.getSuccessCount())
                .failureCount(result.getFailureCount())
                .invalidCount(invalidCount)
                .build();
    }
}
