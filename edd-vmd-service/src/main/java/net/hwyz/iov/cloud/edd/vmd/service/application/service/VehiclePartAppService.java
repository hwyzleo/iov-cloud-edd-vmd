package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehiclePartAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleByDeviceDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehiclePartDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehiclePartQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehiclePartBindingPublisher;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartBindingConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BindingChangeType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.DeviceBindingFact;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehiclePartBindResult;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehiclePartCmd;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.VehicleBindingState;

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
     * 按设备标识查询当前 active 绑定的车辆（VMD-DSN-CR-076，只读）
     * <p>
     * 三态结论：
     * <ul>
     *   <li>零条聚合行 → {@link VehicleBindingState#NO_ACTIVE_BINDING}</li>
     *   <li>唯一 VIN → {@link VehicleBindingState#BOUND}，返回 vin、factRef、updatedAt</li>
     *   <li>多个不同 VIN → {@link VehicleBindingState#BINDING_CONFLICT}，不返回任何 VIN</li>
     * </ul>
     * 只读：不发布事件、不写时间线、不写首次激活。
     * </p>
     *
     * @param deviceId       设备序列号
     * @param deviceCategory 设备类别
     * @return 设备→当前绑定车辆结果
     */
    public VehicleByDeviceDto queryVehicleByDevice(String deviceId, String deviceCategory) {
        List<DeviceBindingFact> facts =
                vehiclePartRepository.selectActiveBindingFactsBySnAndDeviceCategory(deviceId, deviceCategory);
        if (facts == null || facts.isEmpty()) {
            return VehicleByDeviceDto.builder()
                    .bindingState(VehicleBindingState.NO_ACTIVE_BINDING)
                    .build();
        }
        if (facts.size() > 1) {
            // 数据冲突不返回任何 VIN，避免调用方任选导致错绑，统一转人工核验
            log.warn("设备[{}]类别[{}]存在{}个不同VIN的active绑定，判定为绑定冲突",
                    maskDeviceId(deviceId), deviceCategory, facts.size());
            return VehicleByDeviceDto.builder()
                    .bindingState(VehicleBindingState.BINDING_CONFLICT)
                    .build();
        }
        DeviceBindingFact fact = facts.get(0);
        return VehicleByDeviceDto.builder()
                .bindingState(VehicleBindingState.BOUND)
                .vin(fact.getVin())
                .factRef(buildFactRef(fact))
                .updatedAt(fact.getUpdatedAt())
                .build();
    }

    /**
     * 构建绑定事实聚合摘要（不暴露自增主键与内部 seq）
     *
     * @param fact 绑定事实聚合
     * @return factRef 摘要
     */
    private String buildFactRef(DeviceBindingFact fact) {
        StringBuilder factRef = new StringBuilder("vehiclePartBinding:")
                .append(fact.getVin())
                .append(':')
                .append(fact.getBindingCount());
        if (fact.getUpdatedAt() != null) {
            factRef.append(':').append(fact.getUpdatedAt());
        }
        return factRef.toString();
    }

    /**
     * 设备标识脱敏（保留前 4 与后 4 位）
     *
     * @param deviceId 设备标识
     * @return 脱敏后的设备标识
     */
    private String maskDeviceId(String deviceId) {
        if (deviceId == null || deviceId.length() <= 8) {
            return "****";
        }
        return deviceId.substring(0, 4) + "****" + deviceId.substring(deviceId.length() - 4);
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
     * 根据车架号获取所有活跃绑定
     *
     * @param vin 车架号
     * @return 活跃绑定列表
     */
    public List<VehiclePart> getActiveBindingsByVin(String vin) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", vin);
        map.put("bindState", 1);
        return vehiclePartRepository.selectByMap(map);
    }

    /**
     * 根据车架号和车载节点代码获取活跃绑定实体
     *
     * @param vin 车架号
     * @param vehicleNodeCode 车载节点代码
     * @return 绑定关系实体，不存在返回 null
     */
    public VehiclePart getActiveBindingByVinAndNodeCode(String vin, String vehicleNodeCode) {
        return vehiclePartRepository.selectActiveByVinAndVehicleNodeCode(vin, vehicleNodeCode);
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
     * 幂等绑定车辆零件（对齐 VMD-DSN-CR-029 F9 时序与 §3.3 幂等约束）
     * <p>
     * 绑定前先做幂等与冲突检查：
     * 1. 按零件实例 partId 查 active 绑定（一零件一车不变量）：
     *    - 已绑定同一 VIN → 幂等命中，返回 {@link VehiclePartBindResult#SKIPPED_IDEMPOTENT}（视为成功）
     *    - 已绑定其他 VIN → 抛 {@link PartBindingConflictException}
     * 2. 按 (vin, vehicle_node_code) 查装车槽位（同一装车槽位仅一条 active 绑定）：
     *    - 槽位已被其他零件实例占用 → 抛 {@link PartBindingConflictException}
     *    - 槽位已被同一零件实例占用 → 幂等命中，返回 {@link VehiclePartBindResult#SKIPPED_IDEMPOTENT}
     * 3. 未绑定 → 新建 active 绑定并发布绑定变更事件，返回 {@link VehiclePartBindResult#BOUND}
     *
     * @param vehiclePart 绑定关系（含 vin、partId、vehicleNodeCode）
     * @return 绑定结果
     * @throws PartBindingConflictException 零件已绑定其他VIN或装车槽位被其他零件占用
     */
    public VehiclePartBindResult bindVehiclePartIdempotent(VehiclePart vehiclePart) {
        // 1. 按零件实例查 active 绑定（一零件一车不变量）
        VehiclePart boundByPart = vehiclePartRepository.selectActiveByPartId(vehiclePart.getPartId());
        if (boundByPart != null) {
            if (boundByPart.getVin().equalsIgnoreCase(vehiclePart.getVin())) {
                log.info("零件实例[id={}]已绑定车辆[{}]，幂等命中跳过", vehiclePart.getPartId(), vehiclePart.getVin());
                return VehiclePartBindResult.SKIPPED_IDEMPOTENT;
            }
            throw new PartBindingConflictException(
                    String.format("零件实例[id=%s]已绑定车辆[%s]，无法绑定到车辆[%s]",
                            vehiclePart.getPartId(), boundByPart.getVin(), vehiclePart.getVin()));
        }

        // 2. 按 (vin, 车载节点) 查装车槽位（同一装车槽位仅一条 active 绑定）
        VehiclePart boundBySlot = vehiclePartRepository.selectActiveByVinAndVehicleNodeCode(
                vehiclePart.getVin(), vehiclePart.getVehicleNodeCode());
        if (boundBySlot != null) {
            if (boundBySlot.getPartId().longValue() == vehiclePart.getPartId().longValue()) {
                // 同一零件实例已绑定该槽位（第一步已拦截，兜底防御）
                log.info("零件实例[id={}]已绑定车辆[{}]车载节点[{}]，幂等命中跳过",
                        vehiclePart.getPartId(), vehiclePart.getVin(), vehiclePart.getVehicleNodeCode());
                return VehiclePartBindResult.SKIPPED_IDEMPOTENT;
            }
            throw new PartBindingConflictException(
                    String.format("车辆[%s]车载节点[%s]槽位已被零件实例[id=%s]占用",
                            vehiclePart.getVin(), vehiclePart.getVehicleNodeCode(), boundBySlot.getPartId()));
        }

        // 3. 新建 active 绑定并发布事件
        bindVehiclePart(vehiclePart);
        return VehiclePartBindResult.BOUND;
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
