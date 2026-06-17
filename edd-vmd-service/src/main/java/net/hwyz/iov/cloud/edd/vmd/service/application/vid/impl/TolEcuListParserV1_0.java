package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInfoAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.VehicleImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartBindingConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartInstanceState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 总装上线 ECU 清单数据解析器 V1.0
 * <p>
 * 解析 TOL 类型的 ECU 清单数据，完成 ECU↔VIN 绑定。
 * 复用六步内核和零件绑定域。
 *
 * @author hwyz_leo
 * @since 2026-06-17
 */
@Slf4j
@RequiredArgsConstructor
@Component("tolEcuListParserV1.0")
public class TolEcuListParserV1_0 extends BaseProcessor implements VehicleImportDataParser {

    private final ImportDataParserRegistry parserRegistry;
    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final PartInfoAppService partInfoAppService;
    private final VehiclePartAppService vehiclePartAppService;

    @PostConstruct
    public void init() {
        parserRegistry.register(this);
    }

    @Override
    public String getType() {
        return "TOL";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public ImportResult parse(String batchNum, JSONObject dataJson) {
        JSONObject data = getData(dataJson);
        JSONArray items = data.getJSONArray("ITEMS");
        int totalCount = items.size();
        int successCount = 0;
        int failureCount = 0;
        int invalidCount = 0;
        StringBuilder descriptionBuilder = new StringBuilder();

        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            String vin = itemJson.getStr("VIN");
            String ecuSn = itemJson.getStr("ECU_SN");
            String partCode = itemJson.getStr("PART_CODE");
            String vehicleNodeCode = itemJson.getStr("VEHICLE_NODE_CODE");
            String position = itemJson.getStr("POSITION");

            // 校验必填字段
            if (StrUtil.isBlank(vin) || StrUtil.isBlank(ecuSn) || StrUtil.isBlank(partCode)) {
                invalidCount++;
                log.warn("TOL导入数据批次号[{}]存在无效记录: VIN={}, ECU_SN={}, PART_CODE={}",
                        batchNum, vin, ecuSn, partCode);
                continue;
            }

            try {
                // 校验 VIN 存在性
                VehicleBasicInfo vehicleBasicInfo = vehBasicInfoRepository.selectByVin(vin);
                if (vehicleBasicInfo == null) {
                    throw new VehicleNotExistException(vin);
                }

                // 创建 ECU 零件实例
                PartInfo partInfo = PartInfo.builder()
                        .partCode(partCode)
                        .sn(ecuSn)
                        .vehicleNodeCode(vehicleNodeCode)
                        .instanceState(PartInstanceState.IN_USE.value)
                        .build();
                partInfoAppService.upsertPartInfo(partInfo);

                // 绑定 ECU↔VIN
                VehiclePart vehiclePart = VehiclePart.builder()
                        .vin(vin)
                        .partId(partInfo.getId())
                        .vehicleNodeCode(vehicleNodeCode)
                        .deviceItem(position)
                        .bindOrg("TOL")
                        .build();
                vehiclePartAppService.bindVehiclePart(vehiclePart);

                successCount++;
                log.debug("TOL导入数据批次号[{}]车辆[{}]ECU[{}]绑定成功", batchNum, vin, ecuSn);
            } catch (VehicleNotExistException e) {
                failureCount++;
                String errorMsg = String.format("VIN[%s]不存在", vin);
                appendDescription(descriptionBuilder, errorMsg);
                log.warn("TOL导入数据批次号[{}]: {}", batchNum, errorMsg);
            } catch (PartBindingConflictException e) {
                failureCount++;
                String errorMsg = String.format("ECU[%s]已绑定其他VIN", ecuSn);
                appendDescription(descriptionBuilder, errorMsg);
                log.warn("TOL导入数据批次号[{}]: {}", batchNum, errorMsg);
            } catch (Exception e) {
                failureCount++;
                String errorMsg = String.format("VIN[%s]ECU[%s]处理失败: %s", vin, ecuSn, e.getMessage());
                appendDescription(descriptionBuilder, errorMsg);
                log.warn("TOL导入数据批次号[{}]: {}", batchNum, errorMsg, e);
            }
        }

        if (invalidCount > 0) {
            log.warn("TOL导入数据批次号[{}]存在无效记录[{}]", batchNum, invalidCount);
        }

        return ImportResult.builder()
                .totalCount(totalCount)
                .successCount(successCount)
                .failureCount(failureCount)
                .invalidCount(invalidCount)
                .description(descriptionBuilder.length() > 0 ? descriptionBuilder.toString() : null)
                .build();
    }

    /**
     * 追加描述信息，自动截断
     */
    private void appendDescription(StringBuilder sb, String message) {
        if (sb.length() > 0) {
            sb.append("; ");
        }
        sb.append(message);
    }
}
