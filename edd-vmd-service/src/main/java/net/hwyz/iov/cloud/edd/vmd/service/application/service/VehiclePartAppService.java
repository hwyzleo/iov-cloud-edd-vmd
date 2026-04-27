package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehiclePartAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehiclePartDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehiclePartQuery;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆零件应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehiclePartAppService {

    private final VehiclePartRepository vehiclePartRepository;

    /**
     * 查询车辆零件信息
     *
     * @param query 查询 DTO
     * @return 车辆零件 DTO 列表
     */
    public List<VehiclePartDto> search(VehiclePartQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", query.getVin());
        map.put("pn", query.getPn());
        map.put("sn", query.getSn());
        map.put("partState", query.getPartState());
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<VehiclePart> vehiclePartList = vehiclePartRepository.selectByMap(map);
        return PageUtil.convert(vehiclePartList, VehiclePartAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查零件号和序列号是否唯一
     *
     * @param id 主键ID
     * @param pn 零件号
     * @param sn 序列号
     * @return 结果
     */
    public Boolean checkPnAndSnUnique(Long id, String pn, String sn) {
        if (ObjUtil.isNull(id)) {
            id = -1L;
        }
        VehiclePart vehiclePart = vehiclePartRepository.selectByPnAndSn(pn, sn);
        return !ObjUtil.isNotNull(vehiclePart) || vehiclePart.getId().longValue() == id.longValue();
    }

    /**
     * 根据主键ID获取车辆零件信息
     *
     * @param id 主键ID
     * @return 车辆零件 DTO
     */
    public VehiclePartDto getVehiclePartById(Long id) {
        return VehiclePartAssembler.INSTANCE.fromDomain(vehiclePartRepository.selectById(id));
    }

    /**
     * 创建车辆零件
     *
     * @param vehiclePartList 车辆零件列表
     * @return 结果
     */
    public int createVehiclePart(List<VehiclePart> vehiclePartList) {
        return vehiclePartRepository.batchInsert(vehiclePartList);
    }

    /**
     * 新增车辆零件
     *
     * @param vehiclePartDto 车辆零件信息 DTO
     * @param userId        操作用户ID
     * @return 结果
     */
    public int createVehiclePart(VehiclePartDto vehiclePartDto, String userId) {
        VehiclePart vehiclePart = VehiclePartAssembler.INSTANCE.toDomain(vehiclePartDto);
        return vehiclePartRepository.insert(vehiclePart);
    }

    /**
     * 修改车辆零件
     *
     * @param vehiclePartDto 车辆零件信息 DTO
     * @param userId        操作用户ID
     * @return 结果
     */
    public int modifyVehiclePart(VehiclePartDto vehiclePartDto, String userId) {
        VehiclePart vehiclePart = VehiclePartAssembler.INSTANCE.toDomain(vehiclePartDto);
        return vehiclePartRepository.update(vehiclePart);
    }

    /**
     * 批量删除车辆零件
     *
     * @param ids 车辆零件ID数组
     * @return 结果
     */
    public int deleteVehiclePartByIds(Long[] ids) {
        return vehiclePartRepository.batchPhysicalDelete(ids);
    }

    /**
     * 绑定车辆零件
     *
     * @param vehiclePart 车辆零件
     */
    public void bindVehiclePart(VehiclePart vehiclePart) {
        vehiclePart.setPartState(1); // 1-在用
        vehiclePart.setBindTime(Instant.now());
        vehiclePartRepository.insert(vehiclePart);
    }

}
