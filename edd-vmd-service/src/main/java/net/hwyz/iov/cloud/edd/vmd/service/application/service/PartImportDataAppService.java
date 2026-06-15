package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PartImportDataCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.PartImportDataQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartImportDataDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.ProduceDataParserV1_0;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import cn.hutool.core.util.ObjUtil;
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
    private final ProduceDataParserV1_0 produceDataParserV1_0;

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
        // TODO: implement
        return null;
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
     * 解析零件导入数据
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
            // 其他类型正常处理
            ImportDataParser parser = parserRegistry.getParser(partCode, version);
            JSONObject dataJson = JSONUtil.parseObj(partImportData.getData());
            result = parser.parse(batchNum, dataJson);
        }
        
        // 标记为已处理
        partImportData.setHandle(true);
        partImportDataRepository.update(partImportData);
        
        log.info("导入数据解析完成, batchNum={}", batchNum);
        return result;
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
