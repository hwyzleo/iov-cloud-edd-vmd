package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehicleImportDataAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleImportDataDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehicleImportDataQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.ProduceDataParserV1_0;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleImportDataCmd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆导入数据应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleImportDataAppService {

    private final ImportDataParserRegistry parserRegistry;
    private final VehImportDataRepository vehImportDataRepository;
    private final ProduceDataParserV1_0 produceDataParserV1_0;

    /**
     * 查询车辆导入数据信息
     *
     * @param query 查询 DTO
     * @return 车辆导入数据 DTO 列表
     */
    public List<VehicleImportDataDto> search(VehicleImportDataQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("batchNum", query.getBatchNum());
        map.put("type", query.getType());
        map.put("handle", query.getHandle());
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<VehicleImportData> vehicleImportDataList = vehImportDataRepository.selectByMap(map);
        return PageUtil.convert(vehicleImportDataList, VehicleImportDataAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查批次号是否唯一
     *
     * @param id       主键ID
     * @param batchNum 批次号
     * @return 结果
     */
    public Boolean checkBatchNumUnique(Long id, String batchNum) {
        if (ObjUtil.isNull(id)) {
            id = -1L;
        }
        VehicleImportData vehicleImportData = vehImportDataRepository.selectByBatchNum(batchNum);
        return !ObjUtil.isNotNull(vehicleImportData) || vehicleImportData.getId().longValue() == id.longValue();
    }

    /**
     * 解析车辆导入数据
     *
     * @param batchNum 批次号
     * @return 导入处理结果
     */
    public ImportResult parseVehicleImportData(String batchNum) {
        VehicleImportData vehicleImportData = vehImportDataRepository.selectByBatchNum(batchNum);
        if (ObjUtil.isNull(vehicleImportData)) {
            log.warn("批次号[{}]对应的导入数据不存在", batchNum);
            return ImportResult.builder().build();
        }
        
        String type = vehicleImportData.getType();
        String version = vehicleImportData.getVersion();
        
        log.info("解析导入数据, batchNum={}, type={}, version={}", batchNum, type, version);
        
        ImportResult result;
        
        // CR-025: PRODUCE 类型由 US-040 独立处理
        if ("PRODUCE".equals(type)) {
            log.info("CR-025: PRODUCE 类型由 US-040 处理, batchNum={}", batchNum);
            JSONObject dataJson = JSONUtil.parseObj(vehicleImportData.getData());
            result = handleProduceImport(batchNum, dataJson);
        } else {
            // 其他类型正常处理
            ImportDataParser parser = parserRegistry.getParser(type, version);
            JSONObject dataJson = JSONUtil.parseObj(vehicleImportData.getData());
            result = parser.parse(batchNum, dataJson);
        }
        
        // 标记为已处理
        vehicleImportData.setHandle(true);
        vehImportDataRepository.update(vehicleImportData);
        
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
        
        // 复用现有的 PRODUCE 处理逻辑
        // 但需要确保不与零件导入混淆
        return produceDataParserV1_0.parse(batchNum, dataJson);
    }

    /**
     * 根据主键ID获取车辆导入数据信息
     *
     * @param id 主键ID
     * @return 车辆导入数据 DTO
     */
    public VehicleImportDataDto getVehicleImportDataById(Long id) {
        return VehicleImportDataAssembler.INSTANCE.fromDomain(vehImportDataRepository.selectById(id));
    }

    /**
     * 新增车辆导入数据
     *
     * @param vehicleImportDataCmd 车辆导入数据信息 DTO
     * @param userId              操作用户ID
     * @return 结果
     */
    public int createVehicleImportData(VehicleImportDataCmd vehicleImportDataCmd, String userId) {
        VehicleImportData vehicleImportData = VehicleImportDataAssembler.INSTANCE.toDomain(vehicleImportDataCmd);
        vehicleImportData.setHandle(false);
        return vehImportDataRepository.insert(vehicleImportData);
    }

    /**
     * 修改车辆导入数据
     *
     * @param vehicleImportDataCmd 车辆导入数据信息 DTO
     * @param userId              操作用户ID
     * @return 结果
     */
    public int modifyVehicleImportData(VehicleImportDataCmd vehicleImportDataCmd, String userId) {
        VehicleImportData vehicleImportData = VehicleImportDataAssembler.INSTANCE.toDomain(vehicleImportDataCmd);
        return vehImportDataRepository.update(vehicleImportData);
    }

    /**
     * 批量删除车辆导入数据
     *
     * @param ids 车辆导入数据ID数组
     * @return 结果
     */
    public int deleteVehicleImportDataByIds(Long[] ids) {
        return vehImportDataRepository.batchPhysicalDelete(ids);
    }

}
