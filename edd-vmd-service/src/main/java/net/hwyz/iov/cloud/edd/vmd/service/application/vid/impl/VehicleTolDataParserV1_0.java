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
 * 车辆总装上线数据解析器 V1.0
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
public class VehicleTolDataParserV1_0 extends BaseProcessor implements VehicleImportDataParser {

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
        if (items == null || items.isEmpty()) {
            log.warn("TOL导入数据批次号[{}]ITEMS为空", batchNum);
            return ImportResult.builder()
                    .totalCount(0)
                    .successCount(0)
                    .failureCount(0)
                    .invalidCount(0)
                    .build();
        }

        int totalParts = 0;
        int successCount = 0;
        int failureCount = 0;
        int invalidCount = 0;
        StringBuilder descriptionBuilder = new StringBuilder();

        // 遍历 ITEMS（每个 ITEM 对应一辆车）
        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            String vin = itemJson.getStr("VIN");
            JSONArray parts = itemJson.getJSONArray("PARTS");

            if (StrUtil.isBlank(vin)) {
                invalidCount++;
                log.warn("TOL导入数据批次号[{}]存在无效记录: VIN为空", batchNum);
                continue;
            }

            if (parts == null || parts.isEmpty()) {
                log.warn("TOL导入数据批次号[{}]车辆[{}]PARTS为空", batchNum, vin);
                continue;
            }

            totalParts += parts.size();

            // 遍历该车的 PARTS（每个 PART 对应一个 ECU/零件）
            for (Object part : parts) {
                JSONObject partJson = JSONUtil.parseObj(part);
                String partNo = partJson.getStr("PART_NO");
                String sn = partJson.getStr("SN");
                String deviceCode = partJson.getStr("DEVICE_CODE");
                String installPosition = partJson.getStr("INSTALL_POSITION");
                String supplierCode = partJson.getStr("SUPPLIER_CODE");
                String hardwareVersion = partJson.getStr("HARDWARE_VERSION");
                String hardwareNo = partJson.getStr("HARDWARE_NO");

                // 校验必填字段
                if (StrUtil.isBlank(partNo) || StrUtil.isBlank(sn) || StrUtil.isBlank(deviceCode)) {
                    invalidCount++;
                    log.warn("TOL导入数据批次号[{}]车辆[{}]存在无效零件记录: PART_NO={}, SN={}, DEVICE_CODE={}",
                            batchNum, vin, partNo, sn, deviceCode);
                    continue;
                }

                try {
                    // 校验 VIN 存在性
                    VehicleBasicInfo vehicleBasicInfo = vehBasicInfoRepository.selectByVin(vin);
                    if (vehicleBasicInfo == null) {
                        throw new VehicleNotExistException(vin);
                    }

                    // 创建零件实例
                    PartInfo partInfo = PartInfo.builder()
                            .partCode(partNo)
                            .sn(sn)
                            .vehicleNodeCode(deviceCode)
                            .supplierCode(supplierCode)
                            .hardwareVer(hardwareVersion)
                            .hardwarePn(hardwareNo)
                            .instanceState(PartInstanceState.IN_USE.value)
                            .build();
                    partInfoAppService.upsertPartInfo(partInfo);

                    // 绑定零件↔VIN
                    VehiclePart vehiclePart = VehiclePart.builder()
                            .vin(vin)
                            .partId(partInfo.getId())
                            .vehicleNodeCode(deviceCode)
                            .deviceItem(installPosition)
                            .bindOrg("TOL")
                            .build();
                    vehiclePartAppService.bindVehiclePart(vehiclePart);

                    successCount++;
                    log.debug("TOL导入数据批次号[{}]车辆[{}]零件[{}]绑定成功", batchNum, vin, partNo);
                } catch (VehicleNotExistException e) {
                    failureCount++;
                    String errorMsg = String.format("VIN[%s]不存在", vin);
                    appendDescription(descriptionBuilder, errorMsg);
                    log.warn("TOL导入数据批次号[{}]: {}", batchNum, errorMsg);
                } catch (PartBindingConflictException e) {
                    failureCount++;
                    String errorMsg = String.format("零件[%s]已绑定其他VIN", partNo);
                    appendDescription(descriptionBuilder, errorMsg);
                    log.warn("TOL导入数据批次号[{}]: {}", batchNum, errorMsg);
                } catch (Exception e) {
                    failureCount++;
                    String errorMsg = String.format("VIN[%s]零件[%s]处理失败: %s", vin, partNo, e.getMessage());
                    appendDescription(descriptionBuilder, errorMsg);
                    log.warn("TOL导入数据批次号[{}]: {}", batchNum, errorMsg, e);
                }
            }
        }

        if (invalidCount > 0) {
            log.warn("TOL导入数据批次号[{}]存在无效记录[{}]", batchNum, invalidCount);
        }

        return ImportResult.builder()
                .totalCount(totalParts)
                .successCount(successCount)
                .failureCount(failureCount)
                .invalidCount(invalidCount)
                .description(descriptionBuilder.length() > 0 ? descriptionBuilder.toString() : null)
                .build();
    }

    /**
     * description 字段最大长度（与数据库列定义一致）
     */
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    /**
     * 追加描述信息，自动截断
     */
    private void appendDescription(StringBuilder sb, String message) {
        if (sb.length() > 0) {
            sb.append("; ");
        }
        if (sb.length() + message.length() > DESCRIPTION_MAX_LENGTH) {
            int remaining = DESCRIPTION_MAX_LENGTH - sb.length() - 3;
            if (remaining > 0) {
                sb.append(message, 0, remaining);
                sb.append("...");
            }
        } else {
            sb.append(message);
        }
    }
}
