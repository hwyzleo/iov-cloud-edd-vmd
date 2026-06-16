package net.hwyz.iov.cloud.edd.vmd.service.application.service;

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
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.ProduceDataParserV1_0;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 零件导入数据应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartImportDataAppService {

    private final ImportDataParserRegistry parserRegistry;
    private final PartImportDataRepository partImportDataRepository;
    private final MdmPartRepository mdmPartRepository;
    private final ProduceDataParserV1_0 produceDataParserV1_0;
    private final DownstreamProcessorRegistry downstreamProcessorRegistry;

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
        List<PartImportData> list = partImportDataRepository.selectList(partImportData);
        return list.stream().map(this::toDto).collect(java.util.stream.Collectors.toList());
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
        
        // PRODUCE 类型由 US-040 独立处理
        if ("PRODUCE".equals(partCode)) {
            log.info("PRODUCE 类型由 US-040 处理, batchNum={}", batchNum);
            JSONObject dataJson = JSONUtil.parseObj(partImportData.getData());
            result = handleProduceImport(batchNum, dataJson);
        } else {
            // 第一段：通用导入阶段
            result = handleGeneralImport(batchNum, partCode, version, partImportData);
            
            // 第二段：下游联动定制化阶段
            if (result.getFailureCount() == 0) {
                result = handleDownstreamLinkage(batchNum, partCode, partImportData, result);
            }
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
        // 根据 partCode 查询 MDM Part 投影，获取 deviceCode
        Part mdmPart = mdmPartRepository.selectByCode(partCode);
        if (ObjUtil.isNull(mdmPart)) {
            log.error("零件编码[{}]在MDM主数据中不存在", partCode);
            return ImportResult.builder().failureCount(1).description("零件编码[" + partCode + "]在MDM主数据中不存在").build();
        }
        
        String deviceCode = mdmPart.getVehicleNodeCode();
        if (StrUtil.isBlank(deviceCode)) {
            log.error("零件编码[{}]对应的设备编码为空", partCode);
            return ImportResult.builder().failureCount(1).description("零件编码[" + partCode + "]对应的设备编码为空").build();
        }
        
        log.info("根据零件编码[{}]找到设备编码[{}]", partCode, deviceCode);
        
        // 根据 deviceCode 选择解析器
        ImportDataParser parser = parserRegistry.getParser(deviceCode, version);
        JSONObject dataJson = JSONUtil.parseObj(partImportData.getData());
        return parser.parse(batchNum, dataJson);
    }

    /**
     * 处理下游联动定制化阶段
     *
     * @param batchNum 批次号
     * @param partCode 零件编码
     * @param partImportData 导入数据
     * @param generalResult 通用导入结果
     * @return 最终导入结果
     */
    private ImportResult handleDownstreamLinkage(String batchNum, String partCode, PartImportData partImportData, ImportResult generalResult) {
        JSONObject dataJson = JSONUtil.parseObj(partImportData.getData());
        String vehicleNodeCode = dataJson.getStr("vehicleNodeCode");
        
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

    /**
     * 处理整车主档批量导入 (US-040)
     * 
     * @param batchNum 批次号
     * @param dataJson 数据JSON
     * @return 导入结果
     */
    public ImportResult handleProduceImport(String batchNum, JSONObject dataJson) {
        log.info("US-040: 处理整车主档导入, batchNum={}", batchNum);
        return produceDataParserV1_0.parse(batchNum, dataJson);
    }

    private PartImportDataDto toDto(PartImportData entity) {
        if (entity == null) {
            return null;
        }
        return PartImportDataDto.builder()
                .id(entity.getId())
                .batchNum(entity.getBatchNum())
                .partCode(entity.getPartCode())
                .version(entity.getVersion())
                .data(entity.getData())
                .handle(entity.getHandle())
                .description(entity.getDescription())
                .createTime(entity.getCreateTime())
                .build();
    }
}
