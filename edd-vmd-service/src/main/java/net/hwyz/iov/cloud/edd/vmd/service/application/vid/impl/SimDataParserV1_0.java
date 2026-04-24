package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.tsp.mno.api.contract.SimExService;
import net.hwyz.iov.cloud.tsp.mno.api.contract.enums.MnoType;
import net.hwyz.iov.cloud.tsp.mno.api.contract.request.BatchImportSimRequest;
import net.hwyz.iov.cloud.tsp.mno.api.feign.service.ExSimService;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleImportDataException;
import org.springframework.stereotype.Component;

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

    private final ExSimService expSimService;

    @Override
    public void parse(String batchNum, JSONObject dataJson) {
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
        List<SimExService> simList = new ArrayList<>();
        JSONArray items = data.getJSONArray("ITEMS");
        int simInvalidCount = 0;
        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            String iccid = itemJson.getStr("ICCID");
            String imsi = itemJson.getStr("IMSI");
            String msisdn = itemJson.getStr("MSISDN");
            if (StrUtil.isBlank(iccid) && StrUtil.isBlank(imsi) && StrUtil.isBlank(msisdn)) {
                simInvalidCount++;
                continue;
            }
            simList.add(SimExService.builder().iccid(iccid).imsi(imsi).msisdn(msisdn).build());
        }
        if (simInvalidCount > 0) {
            log.warn("SIM卡导入数据批次号[{}]存在无效SIM卡数据[{}]", batchNum, simInvalidCount);
        }
        req.setSimList(simList);
        expSimService.batchImport(req);
    }
}
