package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.core.util.ObjUtil;
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
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartImportDataException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.InboundSourceType;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.MnoType;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SIM卡数据解析器V1.0
 *
 * @author hwyz_leo
 */
@Slf4j
@RequiredArgsConstructor
@Component("simDataParserV1.0")
public class SimDataParserV1_0 extends BaseProcessor implements ImportDataParser {

//    private final TspSimService tspSimService;
    private final ImportDataParserRegistry parserRegistry;

    @PostConstruct
    public void init() {
        parserRegistry.register(this);
    }

    @Override
    public String getType() {
        return "SIM";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public ImportResult parse(String batchNum, JSONObject dataJson) {
        JSONObject data = getData(dataJson);
        String mno = data.getStr("MNO");
        if (StrUtil.isBlank(mno)) {
            throw new PartImportDataException("SIM卡导入数据运营商为空, 批次号: " + batchNum);
        }
        MnoType mnoType = MnoType.valOf(mno.toUpperCase());
        if (ObjUtil.isNull(mnoType)) {
            throw new PartImportDataException("SIM卡导入数据运营商[" + mno + "]未识别, 批次号: " + batchNum);
        }

        List<PartInboundRecord> records = new ArrayList<>();
//        List<SimVo> simList = new ArrayList<>();
        int invalidCount = 0;

        JSONArray items = data.getJSONArray("ITEMS");
        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            String iccid = itemJson.getStr("ICCID");
            String imsi = itemJson.getStr("IMSI");
            String msisdn = itemJson.getStr("MSISDN");

            if (StrUtil.isBlank(iccid) && StrUtil.isBlank(imsi) && StrUtil.isBlank(msisdn)) {
                invalidCount++;
                continue;
            }

            // SIM类型使用iccid作为sn
            Map<String, String> extraFields = new HashMap<>();
            extraFields.put("iccid", iccid);
            extraFields.put("imsi", imsi);
            extraFields.put("msisdn", msisdn);
            extraFields.put("mno", mno);

            PartInboundRecord record = buildInboundRecord(null, iccid, "SIM", null, null,
                    null, batchNum, extraFields);
            records.add(record);

//            simList.add(SimVo.builder().iccid(iccid).imsi(imsi).msisdn(msisdn).build());
        }

        // 使用入站内核处理
        PartInboundResult result = partInboundAppService.processInbound(records, InboundSourceType.MES, null);

        if (invalidCount > 0) {
            log.warn("SIM卡导入数据批次号[{}]存在无效SIM卡数据[{}]", batchNum, invalidCount);
        }

        // 调用下游TSP服务
//        if (!simList.isEmpty()) {
//            BatchImportSimRequest req = new BatchImportSimRequest();
//            req.setBatchNum(batchNum);
//            req.setMnoType(mnoType);
//            req.setSimList(simList);
//            tspSimService.batchImport(req);
//        }

        return ImportResult.builder()
                .totalCount(result.getTotalCount())
                .successCount(result.getSuccessCount())
                .failureCount(result.getFailureCount())
                .invalidCount(invalidCount)
                .build();
    }
}
