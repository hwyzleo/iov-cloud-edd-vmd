package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehImportDataCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehImportDataQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehImportDataDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.VehicleImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 车辆导入数据应用服务类
 * <p>
 * 实现车辆数据导入域六步流水线：校验 -> 标准化 -> 幂等 -> 去重 -> 落库 -> 触发事件
 *
 * @author hwyz_leo
 * @since 2026-06-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehImportDataAppService {

    private final VehImportDataRepository vehImportDataRepository;
    private final ImportDataParserRegistry parserRegistry;

    /**
     * description 字段最大长度（与数据库列定义一致）
     */
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    /**
     * 查询车辆导入数据信息
     *
     * @param query 查询 DTO
     * @return 车辆导入数据 DTO 列表
     */
    public List<VehImportDataDto> search(VehImportDataQuery query) {
        VehImportData vehImportData = VehImportData.builder()
                .batchNum(query.getBatchNum())
                .type(query.getType())
                .handle(query.getHandle())
                .build();
        List<VehImportData> list = vehImportDataRepository.selectList(vehImportData);
        return list.stream().map(this::toDto).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 根据主键ID获取车辆导入数据信息
     *
     * @param id 主键ID
     * @return 车辆导入数据 DTO
     */
    public VehImportDataDto getVehImportDataById(Long id) {
        VehImportData vehImportData = vehImportDataRepository.selectById(id);
        return toDto(vehImportData);
    }

    /**
     * 新增车辆导入数据
     *
     * @param cmd 车辆导入数据信息 CMD
     * @param userId 操作用户ID
     * @return 结果
     */
    public int createVehImportData(VehImportDataCmd cmd, String userId) {
        VehImportData vehImportData = VehImportData.builder()
                .batchNum(cmd.getBatchNum())
                .type(cmd.getType())
                .version(cmd.getVersion())
                .data(cmd.getData())
                .handle(false)
                .description(cmd.getDescription())
                .createTime(LocalDateTime.now())
                .build();
        return vehImportDataRepository.insert(vehImportData);
    }

    /**
     * 修改保存车辆导入数据
     *
     * @param cmd 车辆导入数据信息 CMD
     * @param userId 操作用户ID
     * @return 结果
     */
    public int modifyVehImportData(VehImportDataCmd cmd, String userId) {
        VehImportData vehImportData = VehImportData.builder()
                .id(cmd.getId())
                .batchNum(cmd.getBatchNum())
                .type(cmd.getType())
                .version(cmd.getVersion())
                .data(cmd.getData())
                .description(cmd.getDescription())
                .build();
        return vehImportDataRepository.update(vehImportData);
    }

    /**
     * 批量删除车辆导入数据
     *
     * @param ids 主键ID数组
     * @return 结果
     */
    public int deleteVehImportDataByIds(Long[] ids) {
        return vehImportDataRepository.deleteByIds(ids);
    }

    /**
     * 校验批次号是否唯一
     *
     * @param id 主键ID
     * @param batchNum 批次号
     * @return 结果
     */
    public boolean checkBatchNumUnique(Long id, String batchNum) {
        return vehImportDataRepository.checkBatchNumUnique(id, batchNum);
    }

    /**
     * 解析车辆导入数据（六步流水线）
     * <p>
     * Step 1: Validate（校验）
     * Step 2: Standardize（标准化）
     * Step 3: Idempotent check（幂等）
     * Step 4: Deduplicate（去重）
     * Step 5: Persist to veh_import_data（落库）
     * Step 6: Trigger event（触发事件）
     *
     * @param batchNum 批次号
     * @return 导入结果
     */
    public ImportResult parseVehImportData(String batchNum) {
        // Step 1: Validate（校验）- 校验批次号存在性
        VehImportData vehImportData = vehImportDataRepository.selectByBatchNum(batchNum);
        if (ObjUtil.isNull(vehImportData)) {
            log.warn("车辆导入数据解析失败: 批次号[{}]对应的导入数据不存在", batchNum);
            return ImportResult.builder()
                    .failureCount(1)
                    .description("批次号[" + batchNum + "]对应的导入数据不存在")
                    .build();
        }

        String type = vehImportData.getType();
        String version = vehImportData.getVersion();

        log.info("开始解析车辆导入数据, batchNum={}, type={}, version={}", batchNum, type, version);

        // Step 3: Idempotent check（幂等）- 检查是否已处理
        if (Boolean.TRUE.equals(vehImportData.getHandle())) {
            log.info("车辆导入数据已处理, batchNum={}, 跳过", batchNum);
            return ImportResult.builder()
                    .totalCount(0)
                    .successCount(0)
                    .failureCount(0)
                    .description("批次号[" + batchNum + "]已处理，跳过幂等检查")
                    .build();
        }

        ImportResult result;

        try {
            // Step 2: Standardize（标准化）- 解析 JSON 报文
            JSONObject dataJson = JSONUtil.parseObj(vehImportData.getData());

            // Step 4: Deduplicate（去重）- 检查 ITEMS 内部是否有重复
            int duplicateCount = checkDuplicateItems(batchNum, dataJson);
            if (duplicateCount > 0) {
                log.warn("批次号[{}]存在{}条重复记录", batchNum, duplicateCount);
            }

            // Step 6: Trigger event（触发事件）- 调用解析器
            result = triggerParser(batchNum, type, version, dataJson);

            // 只有当没有失败记录时才标记为已处理
            if (result.getFailureCount() == 0) {
                vehImportData.setHandle(true);
            }
            if (result.getDescription() != null) {
                vehImportData.setDescription(truncateDescription(result.getDescription()));
            }
            vehImportDataRepository.update(vehImportData);

        } catch (Exception e) {
            // 解析失败：写入 description（截断到列长限制）
            log.error("车辆导入数据解析异常, batchNum={}", batchNum, e);
            String errorMsg = "解析失败: " + e.getMessage();
            vehImportData.setDescription(truncateDescription(errorMsg));
            vehImportDataRepository.update(vehImportData);

            result = ImportResult.builder()
                    .failureCount(1)
                    .description(errorMsg)
                    .build();
        }

        log.info("车辆导入数据解析完成, batchNum={}, totalCount={}, successCount={}, failureCount={}",
                batchNum, result.getTotalCount(), result.getSuccessCount(), result.getFailureCount());
        return result;
    }

    /**
     * 检查 ITEMS 内部是否有重复
     *
     * @param batchNum 批次号
     * @param dataJson 数据 JSON
     * @return 重复记录数
     */
    private int checkDuplicateItems(String batchNum, JSONObject dataJson) {
        JSONObject request = dataJson.getJSONObject("REQUEST");
        if (request == null) {
            return 0;
        }
        JSONObject data = request.getJSONObject("DATA");
        if (data == null) {
            return 0;
        }
        JSONArray items = data.getJSONArray("ITEMS");
        if (items == null || items.isEmpty()) {
            return 0;
        }

        // 使用 Set 检查重复
        java.util.Set<String> seen = new java.util.HashSet<>();
        int duplicateCount = 0;
        for (Object item : items) {
            JSONObject itemJson = JSONUtil.parseObj(item);
            // 使用 VIN 或其他唯一标识检查重复
            String vin = itemJson.getStr("VIN");
            if (StrUtil.isNotBlank(vin)) {
                if (!seen.add(vin)) {
                    duplicateCount++;
                    log.warn("批次号[{}]存在重复VIN[{}]", batchNum, vin);
                }
            }
        }
        return duplicateCount;
    }

    /**
     * 触发解析器
     *
     * @param batchNum 批次号
     * @param type 数据类型
     * @param version 版本号
     * @param dataJson 数据 JSON
     * @return 导入结果
     */
    private ImportResult triggerParser(String batchNum, String type, String version, JSONObject dataJson) {
        // 获取解析器
        VehicleImportDataParser parser = parserRegistry.getParser(type, version);
        log.info("使用解析器[{}]处理车辆导入数据, batchNum={}", parser.getClass().getSimpleName(), batchNum);

        // 调用解析器
        return parser.parse(batchNum, dataJson);
    }

    /**
     * 截断 description 到列长限制
     *
     * @param description 原始描述
     * @return 截断后的描述
     */
    private String truncateDescription(String description) {
        if (description == null) {
            return null;
        }
        if (description.length() <= DESCRIPTION_MAX_LENGTH) {
            return description;
        }
        return description.substring(0, DESCRIPTION_MAX_LENGTH - 3) + "...";
    }

    /**
     * 实体转 DTO
     *
     * @param entity 实体
     * @return DTO
     */
    private VehImportDataDto toDto(VehImportData entity) {
        if (entity == null) {
            return null;
        }
        return VehImportDataDto.builder()
                .id(entity.getId())
                .batchNum(entity.getBatchNum())
                .type(entity.getType())
                .version(entity.getVersion())
                .data(entity.getData())
                .handle(entity.getHandle())
                .description(entity.getDescription())
                .createTime(entity.getCreateTime())
                .build();
    }
}
