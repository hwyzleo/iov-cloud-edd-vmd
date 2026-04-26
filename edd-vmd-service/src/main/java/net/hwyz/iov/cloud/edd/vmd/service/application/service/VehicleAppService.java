package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehicleAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehicleVoAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehicleDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.aggregate.Vehicle;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleDetail;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePresetOwner;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleRepository;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleHasBindOrderException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehiclePresetOwnerNotMatchException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleWithoutPresetOwnerException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import net.hwyz.iov.cloud.tsp.account.api.contract.Account;
import net.hwyz.iov.cloud.tsp.account.api.feign.service.ExAccountService;
import org.springframework.stereotype.Service;

import java.util.Date;
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
    private final ExAccountService exAccountService;
    private final VehicleRepository vehicleRepository;
    private final VehicleLifecycleAppService vehicleLifecycleAppService;

    /**
     * 查询车辆信息
     *
     * @param vin             车架号
     * @param buildConfigCode 生产配置代码
     * @param beginTime       开始时间
     * @param endTime         结束时间
     * @param isEol           是否下线
     * @param isOrder         是否有订单
     * @return 车辆 VO 列表
     */
    public List<VehicleVo> search(String vin, String buildConfigCode, Date beginTime, Date endTime, Boolean isEol, Boolean isOrder) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", ParamHelper.fuzzyQueryParam(vin));
        map.put("buildConfigCode", buildConfigCode);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        map.put("isEol", isEol);
        map.put("isOrder", isOrder);
        List<VehicleBasicInfo> vehicleBasicInfoList = vehBasicInfoRepository.selectByMap(map);
        return PageUtil.convert(vehicleBasicInfoList, VehicleVoAssembler.INSTANCE::fromDomain);
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
                vehicleLifecycleAppService.deleteVehicleLifecycleByVin(vehicleBasicInfo.getVin());
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

    /**
     * 检查车辆预设车主
     *
     * @param vin       车架号
     * @param accountId 账号ID
     */
    public void checkVehiclePresetOwner(String vin, String accountId) {
        List<VehiclePresetOwner> vehiclePresetOwnerList = vehBasicInfoRepository.selectPresetOwnerByExample(VehiclePresetOwner.builder().vin(vin).build());
        if (vehiclePresetOwnerList.isEmpty()) {
            throw new VehicleWithoutPresetOwnerException(vin);
        }
        VehiclePresetOwner vehiclePresetOwner = vehiclePresetOwnerList.get(0);
        Account account = exAccountService.getAccountInfo(accountId);
        if (!vehiclePresetOwner.getMobile().equals(account.getMobile()) ||
                !vehiclePresetOwner.getCountryRegionCode().equals(account.getCountryRegionCode())) {
            throw new VehiclePresetOwnerNotMatchException(vin, account.getCountryRegionCode(), account.getMobile(),
                    vehiclePresetOwner.getCountryRegionCode(), vehiclePresetOwner.getMobile());
        }
    }
}
