package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleImportDataException;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspSimService;
import net.hwyz.iov.cloud.iov.tsp.api.vo.SimVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.MnoType;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.BatchImportSimRequest;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;

/**
 * SIM卡数据解析器V1.0
 *
 * @author hwyz_leo
 */
@Slf4j
@RequiredArgsConstructor
@Component("simDataParserV1.0")
public class SimDataParserV1_0 extends BaseParser implements ImportDataParser {

    private final TspSimService tspSimService;
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
            throw new VehicleImportDataException(batchNum, "SIM卡导入数据运营商为空");
        }
        MnoType mnoType = MnoType.valOf(mno.toUpperCase());
        if (ObjUtil.isNull(mnoType)) {
            throw new VehicleImportDataException(batchNum, "SIM卡导入数据运营商[" + mno + "]未识别");
        }
        BatchImportSimRequest req = new BatchImportSimRequest();
        req.setBatchNum(batchNum);
        req.setMnoType(mnoType);
        List<SimVo> simList = new ArrayList<>();
        JSONArray items = data.getJSONArray("ITEMS");
        int totalCount = items.size();
        int invalidCount = 0;
        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            String iccid = itemJson.getStr("ICCID");
            String imsi = itemJson.getStr("IMSI");
            String msisdn = itemJson.getStr("MSISDN");
            if (StrUtil.isBlank(iccid) && StrUtil.isBlank(imsi) && StrUtil.isBlank(msisdn)) {
                invalidCount++;
                continue;
            }
            simList.add(SimVo.builder().iccid(iccid).imsi(imsi).msisdn(msisdn).build());
        }
        if (invalidCount > 0) {
            log.warn("SIM卡导入数据批次号[{}]存在无效SIM卡数据[{}]", batchNum, invalidCount);
        }
        int successCount = simList.size();
        req.setSimList(simList);
        tspSimService.batchImport(req);
        return ImportResult.builder()
                .totalCount(totalCount)
                .successCount(successCount)
                .failureCount(0)
                .invalidCount(invalidCount)
                .build();
    }
}
