package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.mdm.api.service.SupplierService;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.SupplierResponse;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartInboundValidateFailedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartNotActiveException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartNotFoundException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartTypeSchemaNotFoundException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.InboundSourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartInstanceState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartTypeSchema;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartTypeSchemaRegistry;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 零件实例统一入站内核应用服务
 * <p>
 * 六步处理流程：校验→标准化→幂等→去重→落库→触发事件
 * 入口①（上游系统对接）和入口②（管理后台导入）共用此内核
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartInboundAppService {

    private final PartInfoAppService partInfoAppService;
    private final VehiclePartAppService vehiclePartAppService;
    private final PartTypeSchemaRegistry partTypeSchemaRegistry;
    private final MdmPartRepository mdmPartRepository;
    private final SupplierService supplierService;

    /**
     * 零件入站处理结果
     */
    @lombok.Data
    @lombok.Builder
    public static class PartInboundResult {
        private int totalCount;
        private int successCount;
        private int failureCount;
        private int invalidCount;
        private List<String> errors;
    }

    /**
     * 零件入站记录
     */
    @lombok.Data
    @lombok.Builder
    public static class PartInboundRecord {
        private String partCode;
        private String sn;
        private String partType;
        private String vehicleNodeCode;
        private String deviceItem;
        private String supplierCode;
        private String batchNum;
        private String sourceEventId;
        private String inboundBatchNo;
        private Map<String, String> extraFields;
    }

    /**
     * 批量入站处理
     *
     * @param records 入站记录列表
     * @param source  入站来源
     * @param vin     车架号（可选，有VIN时建立绑定）
     * @return 处理结果
     */
    public PartInboundResult processInbound(List<PartInboundRecord> records, InboundSourceType source, String vin) {
        int totalCount = records.size();
        int successCount = 0;
        int failureCount = 0;
        int invalidCount = 0;
        List<String> errors = new ArrayList<>();

        for (PartInboundRecord record : records) {
            try {
                // 步骤1: 字段校验
                validateRecord(record);

                // 步骤2: 标准化
                PartInfo partInfo = normalizeRecord(record, source);

                // 步骤3: 幂等upsert
                partInfoAppService.upsertPartInfo(partInfo);

                // 步骤4+5: 去重 + 建立绑定（如果VIN就绪）
                if (StrUtil.isNotBlank(vin) && partInfo.getId() != null) {
                    bindVehiclePart(vin, partInfo, record, source);
                }

                successCount++;
            } catch (PartInboundValidateFailedException e) {
                invalidCount++;
                errors.add("零件[" + record.getPartCode() + ":" + record.getSn() + "]校验失败: " + e.getMessage());
                log.warn("零件入站校验失败: {}", e.getMessage());
            } catch (PartTypeSchemaNotFoundException e) {
                failureCount++;
                errors.add("零件[" + record.getPartCode() + ":" + record.getSn() + "]类型契约不存在: " + e.getMessage());
                log.warn("零件类型契约不存在: {}", e.getMessage());
            } catch (Exception e) {
                failureCount++;
                errors.add("零件[" + record.getPartCode() + ":" + record.getSn() + "]处理异常: " + e.getMessage());
                log.warn("零件入站处理异常", e);
            }
        }

        if (!errors.isEmpty()) {
            log.warn("零件入站批次处理完成，成功[{}]失败[{}]无效[{}]错误{}", successCount, failureCount, invalidCount, errors);
        }

        return PartInboundResult.builder()
                .totalCount(totalCount)
                .successCount(successCount)
                .failureCount(failureCount)
                .invalidCount(invalidCount)
                .errors(errors)
                .build();
    }

    /**
     * 单条入站处理
     *
     * @param record 入站记录
     * @param source 入站来源
     * @param vin    车架号（可选）
     * @return 处理后的零件实例
     */
    public PartInfo processSingleInbound(PartInboundRecord record, InboundSourceType source, String vin) {
        // 步骤1: 字段校验
        validateRecord(record);

        // 步骤2: 标准化
        PartInfo partInfo = normalizeRecord(record, source);

        // 步骤3: 幂等upsert
        partInfoAppService.upsertPartInfo(partInfo);

        // 步骤4+5: 建立绑定
        if (StrUtil.isNotBlank(vin) && partInfo.getId() != null) {
            bindVehiclePart(vin, partInfo, record, source);
        }

        return partInfo;
    }

    /**
     * 步骤1: 字段校验
     * CR-025: 零件类型取自 MDM Part 投影（partCode），废止人工录入类型
     */
    private void validateRecord(PartInboundRecord record) {
        if (StrUtil.isBlank(record.getPartCode())) {
            throw new PartInboundValidateFailedException("零件编码不能为空");
        }

        // 1. 验证 partCode 存在于 MDM Part 投影
        Part mdmPart = mdmPartRepository.selectByPn(record.getPartCode());
        if (mdmPart == null) {
            throw new PartNotFoundException(record.getPartCode());
        }

        // 2. 验证 MDM Part 状态为 ACTIVE
        if (!"ACTIVE".equals(mdmPart.getStatus())) {
            throw new PartNotActiveException(record.getPartCode());
        }

        // 3. 从 MDM Part 投影获取零件类型
        String mdmPartType = mdmPart.getType();
        if (mdmPartType == null || mdmPartType.isBlank()) {
            log.warn("MDM Part 类型为空, partCode={}, 使用默认类型 OTHER", record.getPartCode());
            mdmPartType = "OTHER";
        }

        // CR-025: 覆写 record.partType 为 MDM Part 投影类型，下游 normalizeRecord / bindVehiclePart 直接使用
        record.setPartType(mdmPartType);

        // 校验供应商编码（宽松：仅警告，不阻断）
        if (StrUtil.isNotBlank(record.getSupplierCode())) {
            try {
                SupplierResponse supplier = supplierService.getByCode(record.getSupplierCode());
                if (supplier == null) {
                    log.warn("零件[{}:{}]供应商编码[{}]在MDM主数据中不存在", record.getPartCode(), record.getSn(), record.getSupplierCode());
                }
            } catch (Exception e) {
                log.warn("零件[{}:{}]供应商编码[{}]校验调用MDM服务失败", record.getPartCode(), record.getSn(), record.getSupplierCode(), e);
            }
        }

        // 解析零件类型
        PartType partType = PartType.valOf(record.getPartType());
        if (partType == null) {
            throw new PartTypeSchemaNotFoundException(record.getPartType());
        }

        // 获取类型契约
        PartTypeSchema schema = partTypeSchemaRegistry.getSchema(partType);
        if (schema == null) {
            throw new PartTypeSchemaNotFoundException(record.getPartType());
        }

        // 校验必需字段
        Map<String, String> fields = new HashMap<>();
        fields.put("sn", record.getSn());
        if (record.getExtraFields() != null) {
            fields.putAll(record.getExtraFields());
        }

        List<String> missingFields = schema.validateRequired(fields);
        if (!missingFields.isEmpty()) {
            throw new PartInboundValidateFailedException("缺失必需字段: " + String.join(", ", missingFields));
        }
    }

    /**
     * 步骤2: 标准化记录为PartInfo实体
     */
    private PartInfo normalizeRecord(PartInboundRecord record, InboundSourceType source) {
        PartType partType = PartType.valOf(record.getPartType());
        PartTypeSchema schema = partTypeSchemaRegistry.getSchema(partType);

        // 处理SIM类型的特殊映射
        String sn = record.getSn();
        if (partType == PartType.SIM && StrUtil.isBlank(sn)) {
            // SIM类型使用iccid作为sn
            sn = record.getExtraFields() != null ? record.getExtraFields().get("iccid") : null;
        }

        // 标准化extra字段
        String extra = null;
        if (record.getExtraFields() != null && !record.getExtraFields().isEmpty()) {
            extra = schema.normalizeExtra(new HashMap<>(record.getExtraFields()));
        }

        // 确定vehicleNodeCode
        String vehicleNodeCode = record.getVehicleNodeCode();
        if (StrUtil.isBlank(vehicleNodeCode) && schema != null) {
            vehicleNodeCode = schema.getDefaultVehicleNodeCode();
        }

        return PartInfo.builder()
                .partCode(record.getPartCode())
                .sn(sn)
                .vehicleNodeCode(vehicleNodeCode)
                .supplierCode(record.getSupplierCode())
                .batchNum(record.getBatchNum())
                .extra(extra)
                .instanceState(PartInstanceState.IN_STOCK.value)
                .source(source)
                .partType(partType)
                .inboundBatchNo(record.getInboundBatchNo())
                .sourceEventId(record.getSourceEventId())
                .lastInboundTime(Instant.now())
                .build();
    }

    /**
     * 建立车辆-零件绑定
     */
    private void bindVehiclePart(String vin, PartInfo partInfo, PartInboundRecord record, InboundSourceType source) {
        PartType partType = PartType.valOf(record.getPartType());
        PartTypeSchema schema = partTypeSchemaRegistry.getSchema(partType);

        String deviceItem = record.getDeviceItem();
        if (StrUtil.isBlank(deviceItem) && schema != null) {
            deviceItem = schema.getDefaultDeviceItem();
        }

        VehiclePart vehiclePart = VehiclePart.builder()
                .vin(vin)
                .partId(partInfo.getId())
                .vehicleNodeCode(partInfo.getVehicleNodeCode())
                .deviceItem(deviceItem)
                .bindOrg(source.getValue())
                .build();
        vehiclePartAppService.bindVehiclePart(vehiclePart);
    }
}
