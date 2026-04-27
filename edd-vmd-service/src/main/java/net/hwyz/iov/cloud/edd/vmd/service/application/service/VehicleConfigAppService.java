package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehicleConfigAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehicleConfigItemAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleConfigDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleConfigItemDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehicleConfigQuery;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleConfig;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleConfigItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleConfigRepository;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆配置应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleConfigAppService {

    private final VehicleConfigRepository vehicleConfigRepository;

    /**
     * 查询车辆配置信息
     *
     * @param query 查询 DTO
     * @return 车辆配置 DTO 列表
     */
    public List<VehicleConfigDto> search(VehicleConfigQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", query.getVin());
        map.put("version", query.getVersion());
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<VehicleConfig> vehicleConfigList = vehicleConfigRepository.selectByMap(map);
        return PageUtil.convert(vehicleConfigList, VehicleConfigAssembler.INSTANCE::fromDomain);
    }

    /**
     * 查询车辆配置项信息
     *
     * @param vin     车架号
     * @param version 配置版本
     * @return 车辆配置项 DTO 列表
     */
    public List<VehicleConfigItemDto> searchItem(String vin, String version) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", vin);
        map.put("version", version);
        List<VehicleConfigItem> vehicleConfigItemList = vehicleConfigRepository.selectConfigItemByMap(map);
        return PageUtil.convert(vehicleConfigItemList, VehicleConfigItemAssembler.INSTANCE::fromDomain);
    }

    /**
     * 根据主键ID获取车辆配置信息
     *
     * @param id 主键ID
     * @return 车辆配置 DTO
     */
    public VehicleConfigDto getVehicleConfigById(Long id) {
        return VehicleConfigAssembler.INSTANCE.fromDomain(vehicleConfigRepository.selectById(id));
    }

    /**
     * 根据主键ID获取车辆配置项信息
     *
     * @param id 主键ID
     * @return 车辆配置项 DTO
     */
    public VehicleConfigItemDto getVehicleConfigItemById(Long id) {
        return VehicleConfigItemAssembler.INSTANCE.fromDomain(vehicleConfigRepository.selectConfigItemById(id));
    }

}
