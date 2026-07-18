package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessor;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.idk.api.service.IdkBtmInfoService;
import net.hwyz.iov.cloud.iov.idk.api.vo.BtmVo;
import net.hwyz.iov.cloud.iov.idk.api.vo.request.BatchImportBtmRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BLE-UWB双模智能进入及启动控制器下游处理器
 * 处理BLE-UWB双模智能进入及启动控制器相关的下游联动
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PepsComboDownstreamProcessor extends BaseProcessor implements DownstreamProcessor {

    private final IdkBtmInfoService idkBtmInfoService;

    @Override
    public void process(String batchNum, String partCode, String vehicleNodeCode, JSONObject data) {
        log.info("PEPS_COMBO处理器处理零件导入下游联动, batchNum={}, partCode={}, vehicleNodeCode={}", 
                batchNum, partCode, vehicleNodeCode);

        String supplier = getSupplier(data);
        if (StrUtil.isBlank(supplier)) {
            log.warn("蓝牙模块导入数据批次号[{}]供应商代码为空", batchNum);
        }
        JSONObject dataContent = getData(data);
        JSONArray items = dataContent.getJSONArray("ITEMS");

        List<PartInboundAppService.PartInboundRecord> records = new ArrayList<>();
        List<BtmVo> btmList = new ArrayList<>();
        int invalidCount = 0;

        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            String pn = itemJson.getStr("ASSEMBLY_PART_NO");
            if (StrUtil.isBlank(pn)) {
                pn = itemJson.getStr("NO");
            }
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

            PartInboundAppService.PartInboundRecord record = buildInboundRecord(pn, sn, "BTM", "BTM_M", "BTM",
                    supplier, batchNum, extraFields);
            records.add(record);

            btmList.add(BtmVo.builder()
                    .sn(sn)
                    .no(pn)
                    .hsm(hsm)
                    .mac(mac)
                    .build());
        }

        if (invalidCount > 0) {
            log.warn("蓝牙模块导入数据批次号[{}]存在无效蓝牙模块数据[{}]", batchNum, invalidCount);
        }

        // 调用下游IDK服务
        if (!btmList.isEmpty()) {
            BatchImportBtmRequest request = new BatchImportBtmRequest();
            request.setBatchNum(batchNum);
            request.setSupplierCode(supplier);
            request.setBtmList(btmList);
            idkBtmInfoService.batchImport(request);
        }
        
        log.info("PEPS_COMBO处理器处理完成, batchNum={}, partCode={}", batchNum, partCode);
    }

    @Override
    public String getSupportedVehicleNodeCode() {
        return "PEPS_COMBO";
    }
}
