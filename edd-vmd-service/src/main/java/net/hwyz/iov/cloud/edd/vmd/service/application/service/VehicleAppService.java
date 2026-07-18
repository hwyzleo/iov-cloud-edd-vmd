package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehicleAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehicleQuery;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.aggregate.Vehicle;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleDetail;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleRepository;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleHasBindOrderException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleConfigRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleOptionRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehSecurityConstantRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleAppService {

    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleLifecycleAppService vehicleLifecycleAppService;
    private final VehiclePartRepository vehiclePartRepository;
    private final VehicleConfigRepository vehicleConfigRepository;
    private final VehicleOptionRepository vehicleOptionRepository;
    private final VehSecurityConstantRepository vehSecurityConstantRepository;

    /**
     * 检查车架号是否存在，不存在则抛出异常
     *
     * @param vin 车架号
     */
    public void checkVinExists(String vin) {
        VehicleBasicInfo vehicleBasicInfo = vehBasicInfoRepository.selectByVin(vin);
        if (ObjUtil.isNull(vehicleBasicInfo)) {
            throw new VehicleNotExistException(vin);
        }
    }

    /**
     * 查询车辆信息
     *
     * @param query 查询 DTO
     * @return 车辆 DTO 列表
     */
    public List<VehicleDto> search(VehicleQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", ParamHelper.fuzzyQueryParam(query.getVin()));
        map.put("configurationCode", query.getConfigurationCode());
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        map.put("isEol", query.getIsEol());
        map.put("isOrder", query.getIsOrder());
        List<VehicleBasicInfo> vehicleBasicInfoList = vehBasicInfoRepository.selectByMap(map);
        return PageUtil.convert(vehicleBasicInfoList, VehicleAssembler.INSTANCE::fromBasicInfo);
    }

    /**
     * 检查车架号是否唯一
     *
     * @param vehicleId 车辆ID
     * @param vin       车架号
     * @return 结果
     */
    public Boolean checkVinUnique(Long vehicleId, String vin) {
        if (ObjUtil.isNull(vehicleId)) {
            vehicleId = -1L;
        }
        VehicleBasicInfo vehicleBasicInfo = vehBasicInfoRepository.selectByVin(vin);
        return !ObjUtil.isNotNull(vehicleBasicInfo) || vehicleBasicInfo.getId().longValue() == vehicleId.longValue();
    }

    /**
     * 根据主键ID获取车辆信息
     *
     * @param id 主键ID
     * @return 车辆基础信息
     */
    public VehicleBasicInfo getVehicleById(Long id) {
        return vehBasicInfoRepository.selectById(id);
    }

    /**
     * 根据车架号获取车辆信息
     *
     * @param vin 车架号
     * @return 车辆 DTO
     */
    public VehicleDto getVehicleByVin(String vin) {
        Vehicle vehicle = vehicleRepository.getByVin(vin);
        return VehicleAssembler.INSTANCE.toDto(vehicle);
    }

    /**
     * 根据车架号获取车辆详细信息
     *
     * @param vin 车架号
     * @return 车辆详细信息领域对象列表
     */
    public List<VehicleDetail> getVehicleDetailByVin(String vin) {
        return vehBasicInfoRepository.selectDetailByVin(vin);
    }

    /**
     * 根据车架号获取车辆基础信息
     *
     * @param vin 车架号
     * @return 车辆基础信息
     */
    public VehicleBasicInfo getVehicleBasicInfoByVin(String vin) {
        return vehBasicInfoRepository.selectByVin(vin);
    }

    /**
     * 新增车辆
     *
     * @param vehicleBasicInfo 车辆基础信息
     * @return 结果
     */
    public int createVehicle(VehicleBasicInfo vehicleBasicInfo) {
        return vehBasicInfoRepository.insert(vehicleBasicInfo);
    }

    /**
     * 修改车辆
     *
     * @param vehicleBasicInfo 车辆基础信息
     * @return 结果
     */
    public int modifyVehicle(VehicleBasicInfo vehicleBasicInfo) {
        return vehBasicInfoRepository.update(vehicleBasicInfo);
    }

    /**
     * 批量删除车辆
     *
     * @param ids 车辆ID数组
     * @return 结果
     */
    public int deleteVehicleByIds(Long[] ids) {
        for (Long id : ids) {
            VehicleBasicInfo vehicleBasicInfo = getVehicleById(id);
            if (ObjUtil.isNotNull(vehicleBasicInfo)) {
                String vin = vehicleBasicInfo.getVin();
                log.info("删除车辆[{}]关联数据", vin);
                vehiclePartRepository.physicalDeleteByVin(vin);
                vehicleConfigRepository.physicalDeleteByVin(vin);
                vehicleConfigRepository.physicalDeleteConfigItemByVin(vin);
                vehicleOptionRepository.physicalDeleteByVin(vin);
                vehSecurityConstantRepository.physicalDeleteByVin(vin);
                vehBasicInfoRepository.physicalDeleteDetailByVin(vin);
                vehBasicInfoRepository.physicalDeletePresetOwnerByVin(vin);
                vehicleLifecycleAppService.deleteVehicleLifecycleByVin(vin);
            }
        }
        return vehBasicInfoRepository.batchPhysicalDelete(ids);
    }

    /**
     * 绑定订单
     *
     * @param vin      车架号
     * @param orderNum 订单编号
     */
    public void bindOrder(String vin, String orderNum) {
        log.info("车辆[{}]绑定订单[{}]", vin, orderNum);
        Vehicle vehicle = vehicleRepository.getByVin(vin);
        if (vehicle.hasOrder()) {
            throw new VehicleHasBindOrderException(vin, vehicle.getOrderNum());
        }
        vehicle.bindOrder(orderNum);
        vehicleRepository.save(vehicle);
        vehicleLifecycleAppService.recordBindOrderNode(vin);
    }

}
