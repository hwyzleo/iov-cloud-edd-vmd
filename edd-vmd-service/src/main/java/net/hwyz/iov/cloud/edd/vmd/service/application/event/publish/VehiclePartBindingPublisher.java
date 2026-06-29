package net.hwyz.iov.cloud.edd.vmd.service.application.event.publish;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehiclePartBindingChangedEvent;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BindingChangeType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 车辆-零件绑定关系变更事件发布类
 * <p>
 * 在绑定域 active 状态变化处统一发布事件
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VehiclePartBindingPublisher {

    private final ApplicationContext ctx;
    private final PartInfoRepository partInfoRepository;
    private final MdmVehicleNodeRepository vehicleNodeRepository;

    /**
     * 发布绑定变更事件
     *
     * @param vehiclePart 绑定关系
     * @param changeType  变更类型
     */
    public void publishBindingChanged(VehiclePart vehiclePart, BindingChangeType changeType) {
        log.info("发布车辆[{}]零件绑定变更事件，绑定ID[{}]，变更类型[{}]",
                vehiclePart.getVin(), vehiclePart.getId(), changeType.getValue());

        // 获取零件信息
        PartInfo partInfo = partInfoRepository.selectById(vehiclePart.getPartId());
        String partCode = partInfo != null ? partInfo.getPartCode() : null;
        String sn = partInfo != null ? partInfo.getSn() : null;

        // 获取设备分类
        VehicleNode vehicleNode = vehicleNodeRepository.selectByCode(vehiclePart.getVehicleNodeCode());
        String deviceCategory = vehicleNode != null ? vehicleNode.getDeviceCategory() : null;

        // 确定事件发生时间
        Instant occurredAt = changeType == BindingChangeType.BIND ?
                vehiclePart.getBindTime() : vehiclePart.getUnbindTime();

        // 生成事件序（复用 vehicle_part 主键 id + bind_time / unbind_time + bind_state 组合表达）
        Long seq = generateSeq(vehiclePart);

        VehiclePartBindingChangedEvent event = new VehiclePartBindingChangedEvent(
                vehiclePart.getVin(),
                vehiclePart.getId(),
                partCode,
                sn,
                deviceCategory,
                vehiclePart.getVehicleNodeCode(),
                null,
                null,
                changeType,
                vehiclePart.getReplaceOfBindingId(),
                occurredAt,
                seq
        );

        ctx.publishEvent(event);
    }

    /**
     * 生成事件序
     * <p>
     * 复用 vehicle_part 主键 id + bind_time / unbind_time + bind_state 组合表达
     *
     * @param vehiclePart 绑定关系
     * @return 事件序
     */
    private Long generateSeq(VehiclePart vehiclePart) {
        // 使用绑定ID作为基础，加上时间戳的后几位确保单调递增
        long base = vehiclePart.getId() != null ? vehiclePart.getId() : 0L;
        Instant time = vehiclePart.getBindTime() != null ? vehiclePart.getBindTime() : Instant.now();
        // 取时间戳的后4位作为序号的一部分
        long timePart = time.toEpochMilli() % 10000;
        return base * 10000 + timePart;
    }

}