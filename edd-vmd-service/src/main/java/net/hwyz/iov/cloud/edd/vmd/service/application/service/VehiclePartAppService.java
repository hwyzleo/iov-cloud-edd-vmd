package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehiclePartAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehiclePartDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehiclePartQuery;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehiclePartCmd;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆-零件绑定关系应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehiclePartAppService {

    private final VehiclePartRepository vehiclePartRepository;

    /**
     * 查询绑定关系信息
     *
     * @param query 查询 DTO
     * @return 绑定关系 DTO 列表
     */
    public List<VehiclePartDto> search(VehiclePartQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", query.getVin());
        map.put("code", query.getCode());
        map.put("sn", query.getSn());
        map.put("partState", query.getPartState());
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<VehiclePart> vehiclePartList = vehiclePartRepository.selectByMap(map);
        return PageUtil.convert(vehiclePartList, VehiclePartAssembler.INSTANCE::fromDomain);
    }

    /**
     * 根据主键ID获取绑定关系信息
     *
     * @param id 主键ID
     * @return 绑定关系 DTO
     */
    public VehiclePartDto getVehiclePartById(Long id) {
        return VehiclePartAssembler.INSTANCE.fromDomain(vehiclePartRepository.selectById(id));
    }

    /**
     * 根据车架号和零件实例ID获取活跃绑定
     *
     * @param vin 车架号
     * @param partId 零件实例ID
     * @return 绑定关系 DTO
     */
    public VehiclePartDto getActiveByVinAndPartId(String vin, Long partId) {
        return VehiclePartAssembler.INSTANCE.fromDomain(vehiclePartRepository.selectActiveByVinAndPartId(vin, partId));
    }

    /**
     * 根据车架号和车载节点代码获取活跃绑定
     *
     * @param vin 车架号
     * @param vehicleNodeCode 车载节点代码
     * @return 绑定关系 DTO
     */
    public VehiclePartDto getActiveByVinAndVehicleNodeCode(String vin, String vehicleNodeCode) {
        return VehiclePartAssembler.INSTANCE.fromDomain(vehiclePartRepository.selectActiveByVinAndVehicleNodeCode(vin, vehicleNodeCode));
    }

    /**
     * 根据零件实例ID获取活跃绑定
     *
     * @param partId 零件实例ID
     * @return 绑定关系 DTO
     */
    public VehiclePartDto getActiveByPartId(Long partId) {
        return VehiclePartAssembler.INSTANCE.fromDomain(vehiclePartRepository.selectActiveByPartId(partId));
    }

    /**
     * 创建绑定关系
     *
     * @param vehiclePartList 绑定关系列表
     * @return 结果
     */
    public int createVehiclePart(List<VehiclePart> vehiclePartList) {
        return vehiclePartRepository.batchInsert(vehiclePartList);
    }

    /**
     * 新增绑定关系
     *
     * @param vehiclePartCmd 绑定关系信息 DTO
     * @param userId 操作用户ID
     * @return 结果
     */
    public int createVehiclePart(VehiclePartCmd vehiclePartCmd, String userId) {
        VehiclePart vehiclePart = VehiclePartAssembler.INSTANCE.toDomain(vehiclePartCmd);
        return vehiclePartRepository.insert(vehiclePart);
    }

    /**
     * 修改绑定关系
     *
     * @param vehiclePartCmd 绑定关系信息 DTO
     * @param userId 操作用户ID
     * @return 结果
     */
    public int modifyVehiclePart(VehiclePartCmd vehiclePartCmd, String userId) {
        VehiclePart vehiclePart = VehiclePartAssembler.INSTANCE.toDomain(vehiclePartCmd);
        return vehiclePartRepository.update(vehiclePart);
    }

    /**
     * 批量删除绑定关系
     *
     * @param ids 主键ID数组
     * @return 结果
     */
    public int deleteVehiclePartByIds(Long[] ids) {
        return vehiclePartRepository.batchPhysicalDelete(ids);
    }

    /**
     * 绑定车辆零件（新建 active 绑定）
     *
     * @param vehiclePart 绑定关系
     */
    public void bindVehiclePart(VehiclePart vehiclePart) {
        vehiclePart.setBindState(1); // 1-绑定中
        vehiclePart.setBindTime(Instant.now());
        vehiclePartRepository.insert(vehiclePart);
    }

    /**
     * 解绑车辆零件（设置为 inactive）
     *
     * @param vehiclePart 绑定关系
     */
    public void unbindVehiclePart(VehiclePart vehiclePart) {
        vehiclePart.setBindState(0); // 0-已解绑
        vehiclePart.setUnbindTime(Instant.now());
        vehiclePartRepository.update(vehiclePart);
    }

}
