package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehiclePartAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehiclePartDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehiclePartQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehiclePartBindingPublisher;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BindingChangeType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehiclePartCmd;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final PartInfoRepository partInfoRepository;
    private final VehiclePartBindingPublisher vehiclePartBindingPublisher;

    /**
     * 查询绑定关系信息
     *
     * @param query 查询 DTO
     * @return 绑定关系 DTO 列表
     */
    public List<VehiclePartDto> search(VehiclePartQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", query.getVin());
        map.put("partId", query.getPartId());
        map.put("vehicleNodeCode", query.getVehicleNodeCode());
        map.put("bindState", query.getBindState());
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        map.put("beginId", query.getBeginId());
        map.put("pageSize", query.getPageSize());
        List<VehiclePart> vehiclePartList = vehiclePartRepository.selectByMap(map);
        List<VehiclePartDto> dtoList = PageUtil.convert(vehiclePartList, VehiclePartAssembler.INSTANCE::fromDomain);
        fillPartInfo(dtoList);
        return dtoList;
    }

    /**
     * 根据主键ID获取绑定关系信息
     *
     * @param id 主键ID
     * @return 绑定关系 DTO
     */
    public VehiclePartDto getVehiclePartById(Long id) {
        VehiclePartDto dto = VehiclePartAssembler.INSTANCE.fromDomain(vehiclePartRepository.selectById(id));
        if (dto != null && dto.getPartId() != null) {
            PartInfo partInfo = partInfoRepository.selectById(dto.getPartId());
            if (partInfo != null) {
                dto.setPartCode(partInfo.getPartCode());
                dto.setSn(partInfo.getSn());
            }
        }
        return dto;
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
     * 根据车架号和安装位置查询活跃绑定（补偿绑定语义）
     *
     * @param vin 车架号
     * @param installPosition 安装位置（车载节点代码）
     * @return 绑定关系实体，不存在返回 null
     */
    public VehiclePart findByVinAndPosition(String vin, String installPosition) {
        return vehiclePartRepository.selectActiveByVinAndVehicleNodeCode(vin, installPosition);
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
        // 发布绑定变更事件
        vehiclePartBindingPublisher.publishBindingChanged(vehiclePart, BindingChangeType.BIND);
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
        // 发布绑定变更事件
        vehiclePartBindingPublisher.publishBindingChanged(vehiclePart, BindingChangeType.UNBIND);
    }

    /**
     * 替换车辆零件（新建 active 绑定，并标记原绑定为 inactive）
     *
     * @param newVehiclePart 新绑定关系
     * @param oldBindingId   被替换的绑定ID
     */
    public void replaceVehiclePart(VehiclePart newVehiclePart, Long oldBindingId) {
        // 1. 将原绑定设置为 inactive
        VehiclePart oldVehiclePart = vehiclePartRepository.selectById(oldBindingId);
        if (oldVehiclePart != null) {
            oldVehiclePart.setBindState(0); // 0-已解绑
            oldVehiclePart.setUnbindTime(Instant.now());
            oldVehiclePart.setUnbindReason("REPLACE");
            vehiclePartRepository.update(oldVehiclePart);
        }

        // 2. 创建新绑定
        newVehiclePart.setBindState(1); // 1-绑定中
        newVehiclePart.setBindTime(Instant.now());
        newVehiclePart.setReplaceOfBindingId(oldBindingId);
        vehiclePartRepository.insert(newVehiclePart);

        // 3. 发布绑定变更事件（REPLACE）
        vehiclePartBindingPublisher.publishBindingChanged(newVehiclePart, BindingChangeType.REPLACE);
    }

    /**
     * 填充零件信息（partCode、sn）
     *
     * @param dtoList 绑定关系 DTO 列表
     */
    private void fillPartInfo(List<VehiclePartDto> dtoList) {
        Set<Long> partIds = dtoList.stream()
                .map(VehiclePartDto::getPartId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (partIds.isEmpty()) {
            return;
        }
        Map<Long, PartInfo> partInfoMap = partIds.stream()
                .map(partInfoRepository::selectById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(PartInfo::getId, Function.identity()));
        for (VehiclePartDto dto : dtoList) {
            if (dto.getPartId() != null) {
                PartInfo partInfo = partInfoMap.get(dto.getPartId());
                if (partInfo != null) {
                    dto.setPartCode(partInfo.getPartCode());
                    dto.setSn(partInfo.getSn());
                }
            }
        }
    }

}
