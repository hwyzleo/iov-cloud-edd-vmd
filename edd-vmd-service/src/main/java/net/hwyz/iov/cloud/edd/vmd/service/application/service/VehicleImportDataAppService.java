package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehicleImportDataAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehicleImportDataDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehicleImportDataQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

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

    private final ApplicationContext applicationContext;
    private final VehImportDataRepository vehImportDataRepository;

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
     */
    public void parseVehicleImportData(String batchNum) {
        VehicleImportData vehicleImportData = vehImportDataRepository.selectByBatchNum(batchNum);
        if (ObjUtil.isNull(vehicleImportData)) {
            log.warn("批次号[{}]对应的导入数据不存在", batchNum);
            return;
        }
        String parserBeanName = vehicleImportData.getType().toLowerCase() + "DataParser";
        ImportDataParser importDataParser = applicationContext.getBean(parserBeanName, ImportDataParser.class);
        if (ObjUtil.isNull(importDataParser)) {
            log.error("未找到对应的解析器[{}]", parserBeanName);
            return;
        }
        JSONObject dataJson = JSONUtil.parseObj(vehicleImportData.getData());
        importDataParser.parse(batchNum, dataJson);
        vehicleImportData.setHandle(true);
        vehImportDataRepository.update(vehicleImportData);
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
     * @param vehicleImportDataDto 车辆导入数据信息 DTO
     * @param userId              操作用户ID
     * @return 结果
     */
    public int createVehicleImportData(VehicleImportDataDto vehicleImportDataDto, String userId) {
        VehicleImportData vehicleImportData = VehicleImportDataAssembler.INSTANCE.toDomain(vehicleImportDataDto);
        vehicleImportData.setHandle(false);
        return vehImportDataRepository.insert(vehicleImportData);
    }

    /**
     * 修改车辆导入数据
     *
     * @param vehicleImportDataDto 车辆导入数据信息 DTO
     * @param userId              操作用户ID
     * @return 结果
     */
    public int modifyVehicleImportData(VehicleImportDataDto vehicleImportDataDto, String userId) {
        VehicleImportData vehicleImportData = VehicleImportDataAssembler.INSTANCE.toDomain(vehicleImportDataDto);
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
