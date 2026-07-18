package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PartImportDataCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.PartImportDataQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartImportDataDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessor;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessorRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.InboundSourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleNodeSchemaRegistry;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 零件导入数据应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartImportDataAppService {

    private final PartImportDataRepository partImportDataRepository;
    private final MdmPartRepository mdmPartRepository;
    private final MdmVehicleNodeRepository mdmVehicleNodeRepository;
    private final PartInboundAppService partInboundAppService;
    private final DownstreamProcessorRegistry downstreamProcessorRegistry;
    private final VehicleNodeSchemaRegistry vehicleNodeSchemaRegistry;
    private final PartSecurityPresetAppService partSecurityPresetAppService;

    /**
     * 查询零件导入数据信息
     *
     * @param query 查询 DTO
     * @return 零件导入数据 DTO 列表
     */
    public List<PartImportDataDto> search(PartImportDataQuery query) {
        PartImportData partImportData = PartImportData.builder()
                .batchNum(query.getBatchNum())
                .partCode(query.getPartCode())
                .handle(query.getHandle())
                .build();
        LocalDateTime beginTime = query.getBeginTime() != null ?
                query.getBeginTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null;
        LocalDateTime endTime = query.getEndTime() != null ?
                query.getEndTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null;
        List<PartImportData> list = partImportDataRepository.selectList(partImportData, beginTime, endTime);
        return PageUtil.convert(list, this::toDto);
    }

    /**
     * 根据主键ID获取零件导入数据信息
     *
     * @param id 主键ID
     * @return 零件导入数据 DTO
     */
    public PartImportDataDto getPartImportDataById(Long id) {
        PartImportData partImportData = partImportDataRepository.selectById(id);
        return toDto(partImportData);
    }

    /**
     * 新增零件导入数据
     *
     * @param cmd 零件导入数据信息 CMD
     * @param userId 操作用户ID
     * @return 结果
     */
    public int createPartImportData(PartImportDataCmd cmd, String userId) {
        PartImportData partImportData = PartImportData.builder()
                .batchNum(cmd.getBatchNum())
                .partCode(cmd.getPartCode())
                .version(cmd.getVersion())
                .data(cmd.getData())
                .handle(false)
                .description(cmd.getDescription())
                .createTime(LocalDateTime.now())
                .build();
        return partImportDataRepository.insert(partImportData);
    }

    /**
     * 修改保存零件导入数据
     *
     * @param cmd 零件导入数据信息 CMD
     * @param userId 操作用户ID
     * @return 结果
     */
    public int modifyPartImportData(PartImportDataCmd cmd, String userId) {
        PartImportData partImportData = PartImportData.builder()
                .id(cmd.getId())
                .batchNum(cmd.getBatchNum())
                .partCode(cmd.getPartCode())
                .version(cmd.getVersion())
                .data(cmd.getData())
                .description(cmd.getDescription())
                .build();
        return partImportDataRepository.update(partImportData);
    }

    /**
     * 批量删除零件导入数据
     *
     * @param ids 主键ID数组
     * @return 结果
     */
    public int deletePartImportDataByIds(Long[] ids) {
        return partImportDataRepository.deleteByIds(ids);
    }

    /**
     * 校验批次号是否唯一
     *
     * @param id 主键ID
     * @param batchNum 批次号
     * @return 结果
     */
    public boolean checkBatchNumUnique(Long id, String batchNum) {
        return partImportDataRepository.checkBatchNumUnique(id, batchNum);
    }

    /**
     * 解析零件导入数据（两段式设计）
     *
     * @param batchNum 批次号
     * @return 导入结果
     */
    public ImportResult parsePartImportData(String batchNum) {
        PartImportData partImportData = partImportDataRepository.selectByBatchNum(batchNum);
        if (ObjUtil.isNull(partImportData)) {
            log.warn("批次号[{}]对应的导入数据不存在", batchNum);
            return ImportResult.builder().build();
        }
        
        String partCode = partImportData.getPartCode();
        String version = partImportData.getVersion();
        
        log.info("解析导入数据, batchNum={}, partCode={}, version={}", batchNum, partCode, version);
        
        ImportResult result;
        
        // 第一段：通用导入阶段
        result = handleGeneralImport(batchNum, partCode, version, partImportData);
        
        // 第二段：下游联动定制化阶段
        if (result.getFailureCount() == 0) {
            result = handleDownstreamLinkage(batchNum, partCode, partImportData, result);
        }
        
        // 标记为已处理
        partImportData.setHandle(true);
        if (result.getDescription() != null) {
            partImportData.setDescription(result.getDescription());
        }
        partImportDataRepository.update(partImportData);
        
        log.info("导入数据解析完成, batchNum={}, totalCount={}, successCount={}, failureCount={}", 
                batchNum, result.getTotalCount(), result.getSuccessCount(), result.getFailureCount());
        return result;
    }

    /**
     * 处理通用导入阶段
     *
     * @param batchNum 批次号
     * @param partCode 零件编码
     * @param version 版本
     * @param partImportData 导入数据
     * @return 导入结果
     */
    private ImportResult handleGeneralImport(String batchNum, String partCode, String version, PartImportData partImportData) {
        log.info("通用导入处理, batchNum={}, partCode={}", batchNum, partCode);
        
        // 解析导入数据JSON
        JSONObject dataJson = JSONUtil.parseObj(partImportData.getData());
        
        // 构建通用入站记录（partCode从ITEM数据中提取）
        List<PartInboundAppService.PartInboundRecord> records = buildGeneralInboundRecords(batchNum, dataJson);
        
        if (records.isEmpty()) {
            log.warn("批次号[{}]没有有效的入站记录", batchNum);
            return ImportResult.builder().totalCount(0).successCount(0).failureCount(1).description("没有有效的入站记录").build();
        }
        
        // 调用统一的入站处理（validateRecord会校验partCode在MDM中存在且ACTIVE）
        PartInboundAppService.PartInboundResult inboundResult = partInboundAppService.processInbound(
                records, InboundSourceType.MANUAL, null);
        
        // 转换为ImportResult
        return ImportResult.builder()
                .totalCount(inboundResult.getTotalCount())
                .successCount(inboundResult.getSuccessCount())
                .failureCount(inboundResult.getFailureCount())
                .invalidCount(inboundResult.getInvalidCount())
                .description(inboundResult.getErrors() != null ? String.join("; ", inboundResult.getErrors()) : null)
                .build();
    }
    
    /**
     * 构建通用入站记录
     * <p>
     * partCode从ITEM数据中提取：优先使用ASSEMBLY_PART_NO，没有则使用HARDWARE_PART_NO
     *
     * @param batchNum 批次号
     * @param dataJson 数据JSON
     * @return 入站记录列表
     */
    private List<PartInboundAppService.PartInboundRecord> buildGeneralInboundRecords(String batchNum, JSONObject dataJson) {
        List<PartInboundAppService.PartInboundRecord> records = new ArrayList<>();
        
        // 获取供应商信息（从HEAD部分）
        JSONObject request = dataJson.getJSONObject("REQUEST");
        JSONObject head = (request != null) ? request.getJSONObject("HEAD") : null;
        String supplier = (head != null) ? head.getStr("ACCOUNT") : null;
        
        // 获取DATA部分
        JSONObject data = (request != null) ? request.getJSONObject("DATA") : null;
        if (data == null) {
            log.warn("批次号[{}]数据中没有DATA部分", batchNum);
            return records;
        }
        
        // 获取items数组
        JSONArray items = data.getJSONArray("ITEMS");
        if (items == null || items.isEmpty()) {
            log.warn("批次号[{}]数据中没有ITEMS数组", batchNum);
            return records;
        }
        
        // 通用字段列表（映射到PartInfo表列或用于partCode提取）
        java.util.Set<String> commonFields = java.util.Set.of(
                "SN", "vehicleNodeCode", "deviceItem", "ASSEMBLY_PART_NO");
        
        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            
            // 提取通用字段
            String sn = itemJson.getStr("SN");
            String vehicleNodeCode = itemJson.getStr("vehicleNodeCode");
            String deviceItem = itemJson.getStr("deviceItem");
            
            // 提取partCode：优先ASSEMBLY_PART_NO，没有则使用HARDWARE_PART_NO
            String assemblyPartNo = itemJson.getStr("ASSEMBLY_PART_NO");
            String hardwarePartNo = itemJson.getStr("HARDWARE_PART_NO");
            String partCode = StrUtil.isNotBlank(assemblyPartNo) ? assemblyPartNo : hardwarePartNo;
            
            // 校验必填字段
            if (StrUtil.isBlank(sn)) {
                log.warn("批次号[{}]存在空SN的记录，跳过", batchNum);
                continue;
            }
            if (StrUtil.isBlank(partCode)) {
                log.warn("批次号[{}]存在空零件编码(ASSEMBLY_PART_NO和HARDWARE_PART_NO均为空)的记录，跳过", batchNum);
                continue;
            }
            
            // 如果ITEM中没有vehicleNodeCode，从MDM Part查询
            if (StrUtil.isBlank(vehicleNodeCode)) {
                Part mdmPart = mdmPartRepository.selectByCode(partCode);
                if (mdmPart != null) {
                    vehicleNodeCode = mdmPart.getVehicleNodeCode();
                }
            }
            
            // 提取非通用字段到extraFields
            Map<String, String> extraFields = new HashMap<>();
            for (String key : itemJson.keySet()) {
                if (!commonFields.contains(key)) {
                    String value = itemJson.getStr(key);
                    if (StrUtil.isNotBlank(value)) {
                        extraFields.put(key.toLowerCase(), value);
                    }
                }
            }
            
            // 构建入站记录
            PartInboundAppService.PartInboundRecord record = PartInboundAppService.PartInboundRecord.builder()
                    .partCode(partCode)
                    .sn(sn)
                    .vehicleNodeCode(vehicleNodeCode)
                    .deviceItem(deviceItem)
                    .supplierCode(supplier)
                    .batchNum(batchNum)
                    .extraFields(extraFields.isEmpty() ? null : extraFields)
                    .build();
            
            records.add(record);
        }
        
        return records;
    }

    /**
     * 处理下游联动定制化阶段
     * <p>
     * 两段式设计：与 vehicle_node_code 驱动的 TSP/OTA/IDK 下游联动并列，
     * 作为另一条同步条件分流（命中安全芯片白名单时触发安全常量预置）
     *
     * @param batchNum 批次号
     * @param partCode 零件编码
     * @param partImportData 导入数据
     * @param generalResult 通用导入结果
     * @return 最终导入结果
     */
    private ImportResult handleDownstreamLinkage(String batchNum, String partCode, PartImportData partImportData, ImportResult generalResult) {
        JSONObject dataJson = JSONUtil.parseObj(partImportData.getData());
        
        // 获取DATA部分
        JSONObject request = dataJson.getJSONObject("REQUEST");
        JSONObject data = (request != null) ? request.getJSONObject("DATA") : null;
        if (data == null) {
            log.info("零件编码[{}]的数据中没有DATA部分，跳过下游联动", partCode);
            return generalResult;
        }
        
        // 获取vehicleNodeCode（从MDM Part获取，用于安全常量预置白名单匹配）
        Part mdmPart = mdmPartRepository.selectByCode(partCode);
        String vehicleNodeCode = (mdmPart != null) ? mdmPart.getVehicleNodeCode() : null;
        
        ImportResult result = generalResult;
        
        // 条件分流1：安全常量预置（命中安全芯片白名单时触发）
        result = handleSecurityConstantPreset(batchNum, partCode, vehicleNodeCode, data, result);
        
        // 条件分流2：vehicleNodeCode 驱动的下游联动
        result = handleVehicleNodeDownstream(batchNum, partCode, dataJson, data, result);
        
        return result;
    }
    
    /**
     * 处理安全常量预置条件分流
     * <p>
     * 按 vehicleNodeCode 是否命中 VehicleNodeSchema「需安全常量预置」白名单（如 TBOX）分流
     *
     * @param batchNum 批次号
     * @param partCode 零件编码
     * @param vehicleNodeCode 车辆节点编码
     * @param data DATA部分JSON
     * @param generalResult 通用导入结果
     * @return 处理后的导入结果
     */
    private ImportResult handleSecurityConstantPreset(String batchNum, String partCode, String vehicleNodeCode, 
                                                      JSONObject data, ImportResult generalResult) {
        // 检查车辆节点是否需要安全常量预置
        if (StrUtil.isBlank(vehicleNodeCode) || !vehicleNodeSchemaRegistry.needsSecurityConstantPreset(vehicleNodeCode)) {
            log.debug("零件编码[{}]节点[{}]不需要安全常量预置，跳过", partCode, vehicleNodeCode);
            return generalResult;
        }
        
        log.info("零件编码[{}]节点[{}]命中安全常量白名单，开始预置, batchNum={}", partCode, vehicleNodeCode, batchNum);
        
        // 获取ITEMS数组，逐条处理安全常量预置
        JSONArray items = data.getJSONArray("ITEMS");
        if (items == null || items.isEmpty()) {
            log.warn("批次号[{}]没有有效的ITEMS记录，跳过安全常量预置", batchNum);
            return generalResult;
        }
        
        int presetFailureCount = 0;
        List<String> presetErrors = new ArrayList<>();
        
        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            String sn = itemJson.getStr("SN");
            
            if (StrUtil.isBlank(sn)) {
                continue;
            }
            
            // 提取chipUid（HSM UID字段名由VehicleNodeSchema定义）
            String hsmUidField = vehicleNodeSchemaRegistry.getHsmUidField(vehicleNodeCode);
            String chipUid = (hsmUidField != null) ? itemJson.getStr(hsmUidField) : null;
            
            if (StrUtil.isBlank(chipUid)) {
                log.warn("零件[{}:{}]缺少安全芯片标识(hsmUidField={})，跳过安全常量预置", partCode, sn, hsmUidField);
                continue;
            }
            
            try {
                partSecurityPresetAppService.preset(partCode, sn, chipUid, batchNum, vehicleNodeCode);
                log.debug("零件[{}:{}]安全常量预置成功", partCode, sn);
            } catch (Exception e) {
                presetFailureCount++;
                presetErrors.add("安全常量预置失败[" + partCode + ":" + sn + "]: " + e.getMessage());
                log.warn("零件[{}:{}]安全常量预置异常", partCode, sn, e);
            }
        }
        
        // 合并预置失败结果
        if (presetFailureCount > 0) {
            String errorMsg = String.join("; ", presetErrors);
            String description = generalResult.getDescription();
            if (description != null) {
                description = description + "; " + errorMsg;
            } else {
                description = errorMsg;
            }
            
            return ImportResult.builder()
                    .totalCount(generalResult.getTotalCount())
                    .successCount(generalResult.getSuccessCount())
                    .failureCount(generalResult.getFailureCount() + presetFailureCount)
                    .invalidCount(generalResult.getInvalidCount())
                    .description(description)
                    .build();
        }
        
        log.info("零件编码[{}]安全常量预置完成, batchNum={}", partCode, batchNum);
        return generalResult;
    }
    
    /**
     * 处理vehicleNodeCode驱动的下游联动
     *
     * @param batchNum 批次号
     * @param partCode 零件编码
     * @param dataJson 完整数据JSON
     * @param data DATA部分JSON
     * @param generalResult 通用导入结果
     * @return 处理后的导入结果
     */
    private ImportResult handleVehicleNodeDownstream(String batchNum, String partCode, JSONObject dataJson,
                                                     JSONObject data, ImportResult generalResult) {
        // 从MDM Part查询vehicleNodeCode
        String vehicleNodeCode = null;
        Part mdmPart = mdmPartRepository.selectByCode(partCode);
        if (mdmPart != null) {
            vehicleNodeCode = mdmPart.getVehicleNodeCode();
        }
        
        // 检查vehicleNodeCode是否非空
        if (StrUtil.isBlank(vehicleNodeCode)) {
            log.info("零件编码[{}]的vehicleNodeCode为空，跳过下游联动", partCode);
            return generalResult;
        }
        
        // 从DownstreamProcessorRegistry获取对应处理器
        DownstreamProcessor processor = downstreamProcessorRegistry.getProcessor(vehicleNodeCode);
        if (processor == null) {
            log.warn("车载节点代码[{}]的处理器不存在，跳过下游联动", vehicleNodeCode);
            return generalResult;
        }
        
        // 同步调用processor.process()
        try {
            processor.process(batchNum, partCode, vehicleNodeCode, dataJson);
            log.info("下游联动处理成功, batchNum={}, partCode={}, vehicleNodeCode={}", batchNum, partCode, vehicleNodeCode);
        } catch (Exception e) {
            // 如果失败，捕获异常，写入part_import_data.description，计入failureCount
            log.warn("零件导入[{}]下游联动处理异常, batchNum={}, partCode={}, vehicleNodeCode={}", 
                    batchNum, partCode, vehicleNodeCode, e);
            
            String errorMsg = "[" + vehicleNodeCode + "] 处理失败: " + e.getMessage();
            String description = generalResult.getDescription();
            if (description != null) {
                description = description + "; " + errorMsg;
            } else {
                description = errorMsg;
            }
            
            return ImportResult.builder()
                    .totalCount(generalResult.getTotalCount())
                    .successCount(generalResult.getSuccessCount())
                    .failureCount(generalResult.getFailureCount() + 1)
                    .description(description)
                    .build();
        }
        
        return generalResult;
    }

    private PartImportDataDto toDto(PartImportData entity) {
        if (entity == null) {
            return null;
        }
        
        // 关联查询零件信息
        String partName = null;
        String vehicleNodeCode = null;
        String vehicleNodeName = null;
        
        Part part = mdmPartRepository.selectByCode(entity.getPartCode());
        if (part != null) {
            partName = part.getName();
            vehicleNodeCode = part.getVehicleNodeCode();
            
            // 关联查询车载节点名称
            if (StrUtil.isNotBlank(vehicleNodeCode)) {
                VehicleNode vehicleNode = mdmVehicleNodeRepository.selectByCode(vehicleNodeCode);
                if (vehicleNode != null) {
                    vehicleNodeName = vehicleNode.getName();
                }
            }
        }
        
        return PartImportDataDto.builder()
                .id(entity.getId())
                .batchNum(entity.getBatchNum())
                .partCode(entity.getPartCode())
                .partName(partName)
                .vehicleNodeCode(vehicleNodeCode)
                .vehicleNodeName(vehicleNodeName)
                .version(entity.getVersion())
                .data(entity.getData())
                .handle(entity.getHandle())
                .description(entity.getDescription())
                .createTime(entity.getCreateTime())
                .build();
    }
}
